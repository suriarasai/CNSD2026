from flask import Flask
from flask_cors import CORS
from app.config import Config
from app.database import init_db
import logging
import os

def create_app(config_class=Config):
    app = Flask(__name__)
    app.config.from_object(config_class)
    
    CORS(app, resources={r"/*": {"origins": "*"}})
    
    log_level = os.getenv('LOG_LEVEL', 'INFO')
    os.makedirs('logs', exist_ok=True)
    
    logging.basicConfig(
        level=getattr(logging, log_level),
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler('logs/app.log'),
            logging.StreamHandler()
        ]
    )
    
    app.logger.info('Initializing Travel Booking Application')
    
    init_db(app)
    
    from app.routes import bp as api_bp
    app.register_blueprint(api_bp, url_prefix='/api')
    
    @app.route('/health')
    def health_check():
        return {'status': 'healthy', 'service': 'travel-booking-api'}, 200
    
    return app