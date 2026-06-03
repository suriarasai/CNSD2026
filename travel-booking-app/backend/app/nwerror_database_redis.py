from pymongo import MongoClient
import redis
from flask import current_app, g
import logging

logger = logging.getLogger(__name__)

def get_mongo_db():
    if 'mongo_db' not in g:
        try:
            client = MongoClient(
                current_app.config['MONGO_URL'],
                serverSelectionTimeoutMS=5000
            )
            client.admin.command('ping')
            g.mongo_client = client
            g.mongo_db = client[current_app.config['MONGO_DB']]
            logger.info('MongoDB connection established')
        except Exception as e:
            logger.error(f'MongoDB connection failed: {e}')
            raise
    return g.mongo_db

def get_redis_client():
    if 'redis_client' not in g:
        try:
            g.redis_client = redis.from_url(
                current_app.config['REDIS_URL'],
                decode_responses=True
            )
            g.redis_client.ping()
            logger.info('Redis connection established')
        except Exception as e:
            logger.error(f'Redis connection failed: {e}')
            raise
    return g.redis_client

def close_db(e=None):
    mongo_client = g.pop('mongo_client', None)
    redis_client = g.pop('redis_client', None)
    
    if mongo_client is not None:
        mongo_client.close()
    
    if redis_client is not None:
        redis_client.close()

def init_db(app):
    app.teardown_appcontext(close_db)
    
    with app.app_context():
        try:
            db = get_mongo_db()
            db.bookings.create_index('created_at')
            logger.info('Database indexes created')
        except Exception as e:
            logger.error(f'Failed to initialize database: {e}')
