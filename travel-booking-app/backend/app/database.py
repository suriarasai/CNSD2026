from pymongo import MongoClient
from pymongo.errors import ServerSelectionTimeoutError
import redis
from flask import current_app, g
import logging
import time

logger = logging.getLogger(__name__)

def get_mongo_db():
    """Get MongoDB connection with retry logic"""
    if 'mongo_db' not in g:
        max_retries = 5
        retry_count = 0
        
        while retry_count < max_retries:
            try:
                client = MongoClient(
                    current_app.config['MONGO_URL'],
                    serverSelectionTimeoutMS=5000,
                    connectTimeoutMS=10000
                )
                client.admin.command('ping')
                g.mongo_client = client
                g.mongo_db = client[current_app.config['MONGO_DB']]
                logger.info('MongoDB connection established')
                return g.mongo_db
            except ServerSelectionTimeoutError as e:
                retry_count += 1
                logger.warning(f'MongoDB connection attempt {retry_count}/{max_retries} failed')
                if retry_count < max_retries:
                    time.sleep(2)
                else:
                    raise
    return g.mongo_db

def get_redis_client():
    """Get Redis connection with graceful fallback"""
    if 'redis_client' not in g:
        try:
            g.redis_client = redis.from_url(
                current_app.config['REDIS_URL'],
                decode_responses=True,
                socket_connect_timeout=5,
                socket_keepalive=True,
                health_check_interval=30
            )
            g.redis_client.ping()
            g.redis_available = True
            logger.info('Redis connection established')
        except Exception as e:
            logger.warning(f'Redis unavailable, continuing without cache: {e}')
            g.redis_client = None
            g.redis_available = False
    return g.redis_client

def safe_redis_get(key):
    """Safely get value from Redis"""
    try:
        client = get_redis_client()
        if client and getattr(g, 'redis_available', False):
            return client.get(key)
    except Exception as e:
        logger.debug(f'Redis GET error: {e}')
    return None

def safe_redis_set(key, value, ex=None):
    """Safely set value in Redis with optional expiration"""
    try:
        client = get_redis_client()
        if client and getattr(g, 'redis_available', False):
            if ex:
                client.setex(key, ex, value)
            else:
                client.set(key, value)
            return True
    except Exception as e:
        logger.debug(f'Redis SET error: {e}')
    return False

def safe_redis_delete(key):
    """Safely delete key from Redis"""
    try:
        client = get_redis_client()
        if client and getattr(g, 'redis_available', False):
            client.delete(key)
            return True
    except Exception as e:
        logger.debug(f'Redis DELETE error: {e}')
    return False

def close_db(e=None):
    """Close database connections"""
    mongo_client = g.pop('mongo_client', None)
    redis_client = g.pop('redis_client', None)
    
    if mongo_client is not None:
        mongo_client.close()
    if redis_client is not None:
        try:
            redis_client.close()
        except:
            pass

def init_db(app):
    """Initialize database connections"""
    app.teardown_appcontext(close_db)
    
    with app.app_context():
        try:
            db = get_mongo_db()
            db.bookings.create_index('created_at')
            logger.info('Database indexes created')
            get_redis_client()
        except Exception as e:
            logger.error(f'Failed to initialize database: {e}')
            raise