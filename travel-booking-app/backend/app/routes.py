from flask import Blueprint, request, jsonify
from app.database import get_mongo_db, safe_redis_get, safe_redis_set, safe_redis_delete
from app.models import Booking
from bson import ObjectId
from bson.errors import InvalidId
import json
import logging

bp = Blueprint('api', __name__)
logger = logging.getLogger(__name__)

@bp.route('/bookings', methods=['GET'])
def get_bookings():
    """Get all bookings with Redis caching"""
    try:
        # Step 1: Try to get from Redis cache first
        cached = safe_redis_get('bookings:all')
        if cached:
            logger.info('Returning cached bookings from Redis')
            return jsonify(json.loads(cached)), 200
        
        # Step 2: Cache miss - get from MongoDB
        db = get_mongo_db()
        bookings = list(db.bookings.find().sort('created_at', -1))
        
        # Step 3: Format bookings
        for booking in bookings:
            booking['_id'] = str(booking['_id'])
            if 'created_at' in booking:
                booking['created_at'] = booking['created_at'].isoformat()
            if 'updated_at' in booking:
                booking['updated_at'] = booking['updated_at'].isoformat()
        
        # Step 4: Store in Redis cache (expires in 5 minutes)
        safe_redis_set('bookings:all', json.dumps(bookings), ex=300)
        
        logger.info(f'Retrieved {len(bookings)} bookings from MongoDB and cached')
        return jsonify(bookings), 200
    
    except Exception as e:
        logger.error(f'Error getting bookings: {str(e)}')
        return jsonify({'error': 'Failed to retrieve bookings'}), 500

@bp.route('/bookings', methods=['POST'])
def create_booking():
    """Create a new booking and invalidate cache"""
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({'error': 'No data provided'}), 400
        
        # Validate required fields
        required_fields = ['destination', 'traveler_name', 'start_date', 'end_date']
        missing = [f for f in required_fields if f not in data]
        
        if missing:
            return jsonify({'error': 'Missing fields', 'missing': missing}), 400
        
        # Create booking object
        booking = Booking.from_dict(data)
        
        # Save to MongoDB
        db = get_mongo_db()
        result = db.bookings.insert_one(booking.to_dict())
        
        # Clear Redis cache so next GET fetches fresh data
        safe_redis_delete('bookings:all')
        
        logger.info(f'Created booking: {result.inserted_id} and cleared cache')
        
        return jsonify({
            '_id': str(result.inserted_id),
            'message': 'Booking created successfully'
        }), 201
    
    except Exception as e:
        logger.error(f'Error creating booking: {str(e)}')
        return jsonify({'error': 'Failed to create booking'}), 500

@bp.route('/bookings/<booking_id>', methods=['DELETE'])
def delete_booking(booking_id):
    """Delete a booking and invalidate cache"""
    try:
        # Validate ObjectId
        try:
            obj_id = ObjectId(booking_id)
        except InvalidId:
            return jsonify({'error': 'Invalid ID'}), 400
        
        # Delete from MongoDB
        db = get_mongo_db()
        result = db.bookings.delete_one({'_id': obj_id})
        
        if result.deleted_count == 0:
            return jsonify({'error': 'Booking not found'}), 404
        
        # Clear Redis cache
        safe_redis_delete('bookings:all')
        
        logger.info(f'Deleted booking: {booking_id} and cleared cache')
        return jsonify({'message': 'Deleted successfully'}), 200
    
    except Exception as e:
        logger.error(f'Error deleting booking: {str(e)}')
        return jsonify({'error': 'Failed to delete booking'}), 500

@bp.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({'status': 'healthy'}), 200