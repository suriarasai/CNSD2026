from flask import Flask, jsonify
import requests
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

SERVICES = {
    'backend': 'http://backend:5000/health',
    'redis': 'redis:6379',
    'mongodb': 'mongodb:27017'
}

@app.route('/health')
def health():
    status = {}
    all_healthy = True
    
    for service, endpoint in SERVICES.items():
        try:
            if service == 'backend':
                response = requests.get(endpoint, timeout=5)
                status[service] = {
                    'status': 'healthy' if response.status_code == 200 else 'unhealthy',
                    'code': response.status_code
                }
            else:
                status[service] = {'status': 'healthy'}
        except Exception as e:
            status[service] = {'status': 'unhealthy', 'error': str(e)}
            all_healthy = False
            logger.error(f'{service} health check failed: {e}')
    
    return jsonify({
        'overall': 'healthy' if all_healthy else 'degraded',
        'services': status
    }), 200 if all_healthy else 503

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8081)