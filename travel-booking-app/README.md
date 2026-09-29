# Travel Booking Application

A full-stack travel booking application built with React (frontend) and Flask (backend), containerized using Podman for local development on Windows and macOS.

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
- Podman Compose orchestration (using an installed Compose provider)
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
- Windows 11 or macOS
- Podman installed (with a running Podman machine on Windows/macOS)
- A Compose provider available to `podman compose` (for example, Docker Compose)
- Git (for cloning repository)

### Installation Steps

1. Clone the repository and open a terminal in the project root.
2. Start Podman (if needed):
```sh
podman machine start
```
   If no machine exists, run `podman machine init` first. Install a Compose provider for `podman compose` (Docker Compose or `podman-compose`). Podman installed directly inside WSL does not need a machine.
3. Initialize, build, and start the project:

   **Windows PowerShell:**
```powershell
.\make.bat init
.\make.bat build
.\make.bat up
```

   **macOS Terminal:**
```sh
./make.sh init
./make.sh build
./make.sh up
```

4. Check status with `.\make.bat status` or `./make.sh status`; initial database startup can take longer than 30 seconds.

5. Access the application:
   - Frontend: http://localhost:8080
   - Bookings API through Nginx: http://localhost:8080/api/bookings
   - Backend health endpoint directly: http://localhost:5000/health
   - Monitoring: http://localhost:8081/health

   On Windows, if `localhost` is unreachable, run `podman machine ssh "ip -4 -o addr show eth0"` and substitute your machine's IP (after `inet`, without the subnet suffix) for `localhost`. The IP may change when the machine restarts. The frontend uses `/api` on the same host as the page.

## Available Commands

### Development commands

Run from the project root. In Windows PowerShell, use `.\make.bat`; on macOS or Ubuntu WSL, use `./make.sh`. Both scripts accept the same commands:

```powershell
.\make.bat help            # Show all available commands
.\make.bat build           # Build all container images
.\make.bat up              # Start all services in detached mode
.\make.bat down            # Stop all services
.\make.bat restart         # Restart all services
.\make.bat logs            # Show logs for all services (follow mode)
.\make.bat status          # Display status of all containers
.\make.bat clean           # Remove containers, volumes, and local images
.\make.bat shell-backend   # Open interactive shell in backend container
.\make.bat shell-frontend  # Open interactive shell in frontend container
.\make.bat test            # Run backend tests
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
podman compose logs -f
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

**Create a new booking (macOS Terminal):**
```sh
curl -X POST http://localhost:5000/api/bookings \
  -H "Content-Type: application/json" \
  -d '{"destination":"Paris","traveler_name":"John Doe","start_date":"2026-10-01","end_date":"2026-10-07","num_travelers":2}'
```

**Create a new booking (Windows PowerShell):**
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:5000/api/bookings -ContentType 'application/json' -Body '{"destination":"Paris","traveler_name":"John Doe","start_date":"2026-10-01","end_date":"2026-10-07","num_travelers":2}'
```

If Windows-to-WSL localhost forwarding is unavailable, use the address from installation step 5 instead of `localhost`.

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

To follow backend logs on either platform: `podman logs -f travel-backend`.

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

Recreate the Compose-managed network:
```bash
podman compose down
podman compose up -d
```

### Build fails while pulling images

If pulling from `docker.io` fails with `invalid username/password`, refresh your Docker Hub login (`podman login docker.io`, and Docker Desktop's sign-in if it supplies the Compose provider). For public images, remove stale credentials with `podman logout docker.io` or `docker logout` as appropriate, then retry the build.

### Cannot connect to Podman

On Windows and macOS, check `podman machine list` and run `podman machine start` if needed.

Inside Ubuntu WSL, install `podman` and `podman-compose` with `sudo apt update && sudo apt install -y podman podman-compose`, then use `./make.sh`. If no systemd user bus is available, the script uses Ubuntu's rootful Podman via `sudo` and the project-local cgroupfs fallback. Stop any Windows Podman containers publishing the same ports before starting the Ubuntu instance.

### Cache Issues

Clear Redis cache:
```bash
podman exec travel-redis redis-cli FLUSHDB
```

### Complete Reset

Stop everything and start fresh (macOS/WSL: use `./make.sh` instead of `.\make.bat`):
```powershell
.\make.bat down
.\make.bat clean
.\make.bat build
.\make.bat up
```

## 12-Factor App Compliance

This application follows the twelve-factor methodology:

1. **Codebase:** Single repository with version control
2. **Dependencies:** Explicitly declared in package.json and requirements.txt
3. **Config:** Environment variables in `compose.yaml` (optionally overridden with a local `.env` file)
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
│   ├── mongod.conf         MongoDB configuration
│   └── containers-wsl.conf WSL Podman cgroup fallback
├── logs/                    Application logs
├── compose.yaml            Container orchestration
├── .env                    Optional local environment overrides (not tracked)
├── make.bat                Windows development commands
├── make.sh                 macOS/WSL development commands
└── README.md               This file
```

## Development Workflow

1. Make code changes in `frontend/src` or `backend/app`
2. Changes are automatically reflected (hot reload enabled)
3. Test changes in the browser using the frontend URL from installation step 5
4. Check logs if issues occur: `.\make.bat logs` (Windows) or `./make.sh logs` (macOS)
5. Restart services if needed: `.\make.bat restart` (Windows) or `./make.sh restart` (macOS)

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
- Set a unique `SECRET_KEY` through a local `.env` file or environment variable
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
