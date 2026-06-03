from flask import Blueprint, request, jsonify, current_app
from backend.app.nwerror_database_redis import get_mongo_db, get_redis_client
from app.models import Booking
from bson import ObjectId
import json
import logging

bp = Blueprint('api', __name__)
logger = logging.getLogger(__name__)

@bp.route('/bookings', methods=['GET'])
def get_bookings():
    try:
        redis_client = get_redis_client()
        cached = redis_client.get('bookings:all')
        
        if cached:
            logger.info('Returning cached bookings')
            return jsonify(json.loads(cached)), 200
        
        db = get_mongo_db()
        bookings = list(db.bookings.find().sort('created_at', -1))
        
        for booking in bookings:
            booking['_id'] = str(booking['_id'])
            booking['created_at'] = booking['created_at'].isoformat()
            booking['updated_at'] = booking['updated_at'].isoformat()
        
        redis_client.setex('bookings:all', 300, json.dumps(bookings))
        
        logger.info(f'Retrieved {len(bookings)} bookings')
        return jsonify(bookings), 200
    
    except Exception as e:
        logger.error(f'Error getting bookings: {e}')
        return jsonify({'error': 'Failed to retrieve bookings'}), 500

@bp.route('/bookings', methods=['POST'])
def create_booking():
    try:
        data = request.get_json()
        
        required_fields = ['destination', 'traveler_name', 'start_date', 'end_date']
        if not all(field in data for field in required_fields):
            return jsonify({'error': 'Missing required fields'}), 400
        
        booking = Booking.from_dict(data)
        
        db = get_mongo_db()
        result = db.bookings.insert_one(booking.to_dict())
        
        redis_client = get_redis_client()
        redis_client.delete('bookings:all')
        
        logger.info(f'Created booking: {result.inserted_id}')
        
        return jsonify({
            '_id': str(result.inserted_id),
            'message': 'Booking created successfully'
        }), 201
    
    except Exception as e:
        logger.error(f'Error creating booking: {e}')
        return jsonify({'error': 'Failed to create booking'}), 500

@bp.route('/bookings/<booking_id>', methods=['DELETE'])
def delete_booking(booking_id):
    try:
        db = get_mongo_db()
        result = db.bookings.delete_one({'_id': ObjectId(booking_id)})
        
        if result.deleted_count == 0:
            return jsonify({'error': 'Booking not found'}), 404
        
        redis_client = get_redis_client()
        redis_client.delete('bookings:all')
        
        logger.info(f'Deleted booking: {booking_id}')
        
        return jsonify({'message': 'Booking deleted successfully'}), 200
    
    except Exception as e:
        logger.error(f'Error deleting booking: {e}')
        return jsonify({'error': 'Failed to delete booking'}), 500

@bp.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'healthy',
        'service': 'travel-booking-api'
    }), 200