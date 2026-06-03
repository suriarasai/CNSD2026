import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    SECRET_KEY = os.getenv('SECRET_KEY', 'dev-secret-key')
    REDIS_URL = os.getenv('REDIS_URL', 'redis://localhost:6379/0')
    MONGO_URL = os.getenv('MONGO_URL', 'mongodb://localhost:27017/')
    MONGO_DB = os.getenv('MONGO_DB', 'travel_booking')
    LOG_LEVEL = os.getenv('LOG_LEVEL', 'INFO')
    
    SESSION_TYPE = 'redis'
    SESSION_PERMANENT = False
    SESSION_USE_SIGNER = True
    
    JSON_SORT_KEYS = False
