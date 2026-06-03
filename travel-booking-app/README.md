# Travel Booking Application

A full-stack travel booking application built with React (frontend) and Flask (backend), containerized using Podman for local development on Windows 11.

## Project Overview

This application allows users to create, view, and manage travel bookings. It follows cloud-native practices including the 12-factor app methodology and uses microservices architecture with container orchestration.

### Technology Stack

**Frontend:**
- React 18 with Vite
- Progressive Web App (PWA) support
- Axios for API communication
- Responsive CSS design

**Backend:**
- Python Flask REST API
- MongoDB for persistent data storage
- Redis for caching and session management
- CORS enabled for cross-origin requests

**Infrastructure:**
- Podman containers
- Nginx reverse proxy
- Docker Compose (podman-compose) orchestration
- Health monitoring service

## Container Architecture

### 1. Frontend Container (travel-frontend)
**Port:** 5173  
**Purpose:** Serves the React application using Vite development server.  
**Technology:** Node.js 20 Alpine  
**Volumes:** Source code mounted for hot-reloading during development  

### 2. Backend Container (travel-backend)
**Port:** 5000  
**Purpose:** Flask REST API handling all business logic and data operations.  
**Technology:** Python 3.11 Slim  
**Dependencies:** Flask, Flask-CORS, PyMongo, Redis client  
**Volumes:** Application code and logs directory mounted  

### 3. Redis Container (travel-redis)
**Port:** 6379  
**Purpose:** In-memory data store for caching and session management.  
**Technology:** Redis 7 Alpine  
**Configuration:** 256MB max memory with LRU eviction policy  
**Data Persistence:** Volume mounted to /data for RDB snapshots  

### 4. MongoDB Container (travel-mongodb)
**Port:** 27017  
**Purpose:** Primary database for persistent booking data storage.  
**Technology:** MongoDB 7  
**Database:** travel_booking  
**Data Persistence:** Volume mounted to /data/db  

### 5. Nginx Container (travel-nginx)
**Port:** 8080  
**Purpose:** Reverse proxy routing requests to frontend and backend.  
**Technology:** Nginx Alpine  
**Routes:**
- `/` routes to frontend (port 5173)
- `/api/` routes to backend (port 5000)
- `/health` returns nginx health status

### 6. Monitoring Container (travel-monitoring)
**Port:** 8081  
**Purpose:** Health check aggregation service monitoring all containers.  
**Technology:** Python 3.11 Slim with Flask  
**Monitors:** Backend API, Redis, MongoDB connectivity  

## Application Functionality

### Booking Management
- Create new travel bookings with destination, traveler name, dates, and number of travelers
- View all bookings in a responsive card layout
- Delete existing bookings
- Automatic status assignment (confirmed by default)

### Caching Strategy
- GET requests check Redis cache first (5-minute TTL)
- Cache miss triggers MongoDB query and stores result in Redis
- POST and DELETE operations invalidate cache to ensure data consistency
- Graceful degradation: Application continues to function if Redis is unavailable

### Data Flow
```
User Request → Nginx (8080) → Backend API (5000)
                                    ↓
                            Check Redis Cache
                                    ↓
                          Cache Hit? → Return Data
                                    ↓
                          Cache Miss → Query MongoDB
                                    ↓
                               Cache Result
                                    ↓
                              Return Data
```

## Setup and Installation

### Prerequisites
- Windows 11
- Podman Desktop installed
- Git (for cloning repository)

### Installation Steps

1. Clone or create the project directory structure
2. Navigate to project root
3. Initialize the project:
```batch
make.bat init
```

4. Build all containers:
```batch
make.bat build
```

5. Start all services:
```batch
make.bat up
```

6. Wait 30 seconds for all services to initialize

7. Access the application:
- Frontend: http://localhost:8080
- Backend API: http://localhost:5000
- Monitoring: http://localhost:8081

## Available Commands

### make.bat Commands

```batch
make.bat help            Show all available commands
make.bat build           Build all container images
make.bat up              Start all services in detached mode
make.bat down            Stop all services
make.bat restart         Restart all services
make.bat logs            Show logs for all services (follow mode)
make.bat status          Display status of all containers
make.bat clean           Remove all containers, volumes, and images
make.bat shell-backend   Open interactive shell in backend container
make.bat shell-frontend  Open interactive shell in frontend container
make.bat test            Run backend tests
```

## Monitoring and Logging

### Health Check Endpoints

**Nginx Health Check:**
```bash
curl http://localhost:8080/health
```
Expected Response: `healthy`

**Backend Health Check:**
```bash
curl http://localhost:5000/health
```
Expected Response:
```json
{
  "status": "healthy",
  "service": "travel-booking-api"
}
```

**Monitoring Service Health:**
```bash
curl http://localhost:8081/health
```
Expected Response:
```json
{
  "overall": "healthy",
  "services": {
    "backend": {
      "status": "healthy",
      "code": 200
    },
    "redis": {
      "status": "healthy"
    },
    "mongodb": {
      "status": "healthy"
    }
  }
}
```

### Container Logs

**View all container logs:**
```bash
podman-compose logs -f
```

**View specific container logs:**
```bash
podman logs travel-backend
podman logs travel-frontend
podman logs travel-redis
podman logs travel-mongodb
podman logs travel-nginx
podman logs travel-monitoring
```

**View last N lines of logs:**
```bash
podman logs --tail 50 travel-backend
podman logs --tail 20 travel-redis
```

**Follow logs in real-time:**
```bash
podman logs -f travel-backend
```

**View logs with timestamps:**
```bash
podman logs --timestamps travel-backend
```

**View logs from specific time:**
```bash
podman logs --since 30m travel-backend
podman logs --since "2025-09-30T00:00:00" travel-backend
```

### Container Status and Inspection

**Check container status:**
```bash
podman ps -a --filter "name=travel-"
```

**Inspect specific container:**
```bash
podman inspect travel-backend
podman inspect travel-redis
```

**Check container resource usage:**
```bash
podman stats travel-backend
podman stats --no-stream
```

**View container network information:**
```bash
podman network inspect travel-network
```

### Database Inspection

**Connect to Redis CLI:**
```bash
podman exec -it travel-redis redis-cli
```

**Check Redis keys:**
```bash
podman exec travel-redis redis-cli KEYS "*"
```

**View specific Redis key:**
```bash
podman exec travel-redis redis-cli GET "bookings:all"
```

**Check Redis info:**
```bash
podman exec travel-redis redis-cli INFO stats
podman exec travel-redis redis-cli INFO memory
```

**Connect to MongoDB:**
```bash
podman exec -it travel-mongodb mongosh
```

**Query MongoDB from command line:**
```bash
podman exec travel-mongodb mongosh travel_booking --eval "db.bookings.find()"
podman exec travel-mongodb mongosh travel_booking --eval "db.bookings.countDocuments()"
```

### API Testing

**Get all bookings:**
```bash
curl http://localhost:5000/api/bookings
```

**Create a new booking:**
```bash
curl -X POST http://localhost:5000/api/bookings \
  -H "Content-Type: application/json" \
  -d "{\"destination\":\"Paris\",\"traveler_name\":\"John Doe\",\"start_date\":\"2025-10-01\",\"end_date\":\"2025-10-07\",\"num_travelers\":2}"
```

**Delete a booking (replace BOOKING_ID):**
```bash
curl -X DELETE http://localhost:5000/api/bookings/BOOKING_ID
```

### Network Testing

**Test network connectivity between containers:**
```bash
podman exec travel-backend ping -c 3 redis
podman exec travel-backend ping -c 3 mongodb
```

**Test port connectivity:**
```bash
podman exec travel-backend nc -zv redis 6379
podman exec travel-backend nc -zv mongodb 27017
```

**Test Redis connection from backend:**
```bash
podman exec travel-backend python -c "import redis; r=redis.from_url('redis://redis:6379/0'); print('Redis:', r.ping())"
```

**Test MongoDB connection from backend:**
```bash
podman exec travel-backend python -c "from pymongo import MongoClient; client=MongoClient('mongodb://mongodb:27017/'); print('MongoDB:', client.admin.command('ping'))"
```

## Application Logs

Application-specific logs are stored in the `./logs` directory on the host machine.

**View application log file:**
```bash
type logs\app.log
tail -f logs/app.log
```

**Search logs for errors:**
```bash
findstr /i "error" logs\app.log
findstr /i "redis" logs\app.log
findstr /i "mongodb" logs\app.log
```

## Troubleshooting

### Services Not Starting

Check container status and logs:
```bash
podman ps -a
podman logs travel-backend --tail 50
```

### Database Connection Issues

Test connectivity:
```bash
podman exec travel-redis redis-cli ping
podman exec travel-mongodb mongosh --eval "db.adminCommand('ping')"
```

### Network Issues

Recreate network:
```bash
podman-compose down
podman network rm travel-network
podman network create travel-network
podman-compose up -d
```

### Cache Issues

Clear Redis cache:
```bash
podman exec travel-redis redis-cli FLUSHDB
```

### Complete Reset

Stop everything and start fresh:
```bash
make.bat down
make.bat clean
make.bat build
make.bat up
```

## 12-Factor App Compliance

This application follows the twelve-factor methodology:

1. **Codebase:** Single repository with version control
2. **Dependencies:** Explicitly declared in package.json and requirements.txt
3. **Config:** Environment variables in .env files
4. **Backing Services:** MongoDB and Redis as attached resources
5. **Build, Release, Run:** Separate stages via Containerfiles
6. **Processes:** Stateless application processes
7. **Port Binding:** Services export via port binding
8. **Concurrency:** Horizontal scaling through containers
9. **Disposability:** Fast startup and graceful shutdown
10. **Dev/Prod Parity:** Same containers, different configurations
11. **Logs:** Stdout/stderr streams captured by Podman
12. **Admin Processes:** Shell access via make.bat commands

## Project Structure

```
travel-booking-app/
├── frontend/                 React Progressive Web App
│   ├── src/
│   │   ├── components/      React components
│   │   ├── services/        API service layer
│   │   ├── App.jsx          Main application component
│   │   └── main.jsx         Application entry point
│   ├── public/              Static assets and PWA manifest
│   ├── Containerfile        Frontend container definition
│   └── package.json         Node.js dependencies
├── backend/                  Flask REST API
│   ├── app/
│   │   ├── __init__.py      Application factory
│   │   ├── config.py        Configuration management
│   │   ├── database.py      Database connections
│   │   ├── models.py        Data models
│   │   └── routes.py        API endpoints
│   ├── Containerfile        Backend container definition
│   ├── requirements.txt     Python dependencies
│   └── wsgi.py             WSGI entry point
├── nginx/                   Reverse proxy configuration
│   ├── nginx.conf          Nginx routing rules
│   └── Containerfile       Nginx container definition
├── monitoring/              Health check service
│   ├── healthcheck.py      Monitoring logic
│   └── Containerfile       Monitoring container definition
├── config/                  Service configurations
│   ├── redis.conf          Redis configuration
│   └── mongod.conf         MongoDB configuration
├── logs/                    Application logs
├── compose.yaml            Container orchestration
├── .env                    Environment variables
├── make.bat                Build and deployment script
└── README.md               This file
```

## Development Workflow

1. Make code changes in `frontend/src` or `backend/app`
2. Changes are automatically reflected (hot reload enabled)
3. Test changes in browser at http://localhost:8080
4. Check logs if issues occur: `make.bat logs`
5. Restart services if needed: `make.bat restart`

## Data Persistence

Data is persisted in Docker volumes:
- `redis-data`: Redis RDB snapshots
- `mongodb-data`: MongoDB database files

To backup data, export from MongoDB:
```bash
podman exec travel-mongodb mongodump --db travel_booking --out /data/backup
```

## Security Notes

This is a development environment. For production deployment:
- Change SECRET_KEY in .env file
- Enable authentication for MongoDB and Redis
- Use HTTPS with proper SSL certificates
- Implement user authentication and authorization
- Add input validation and sanitization
- Enable rate limiting
- Configure proper CORS origins
- Use secrets management system
- Implement logging and monitoring alerts

## License

This project is for educational and development purposes.