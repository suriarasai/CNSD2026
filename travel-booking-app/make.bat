@echo off
setlocal enabledelayedexpansion

if "%1"=="" goto help

if "%1"=="help" goto help
if "%1"=="build" goto build
if "%1"=="up" goto up
if "%1"=="down" goto down
if "%1"=="restart" goto restart
if "%1"=="logs" goto logs
if "%1"=="clean" goto clean
if "%1"=="status" goto status
if "%1"=="shell-backend" goto shell_backend
if "%1"=="shell-frontend" goto shell_frontend
if "%1"=="test" goto test
if "%1"=="init" goto init

goto help

:help
echo Travel Booking Application - Development Commands
echo.
echo Usage: make.bat [command]
echo.
echo Commands:
echo   init            - Initialize project (create directories, install deps)
echo   build           - Build all containers
echo   up              - Start all services
echo   down            - Stop all services
echo   restart         - Restart all services
echo   logs            - Show logs for all services
echo   status          - Show status of all containers
echo   clean           - Remove all containers, volumes, and images
echo   shell-backend   - Open shell in backend container
echo   shell-frontend  - Open shell in frontend container
echo   test            - Run tests
echo   help            - Show this help message
goto :eof

:init
echo Initializing project...
if not exist logs mkdir logs
if not exist config mkdir config
if not exist monitoring mkdir monitoring
echo Project initialized!
goto :eof

:build
echo Building containers...
podman compose build
goto :eof

:up
echo Starting services...
podman compose up -d
echo.
echo Services started!
echo Frontend: http://localhost:8080
echo Backend API: http://localhost:8080/api
echo Monitoring: http://localhost:8081
goto :eof

:down
echo Stopping services...
podman compose down
goto :eof

:restart
echo Restarting services...
podman compose restart
goto :eof

:logs
if "%2"=="" (
    podman compose logs -f
) else (
    podman compose logs -f %2
)
goto :eof

:status
echo Container Status:
podman ps -a --filter "name=travel-"
goto :eof

:clean
echo Cleaning up...
podman compose down -v
podman rmi travel-booking-app-frontend travel-booking-app-backend travel-booking-app-nginx travel-booking-app-monitoring 2>nul
echo Cleanup complete!
goto :eof

:shell_backend
podman exec -it travel-backend /bin/sh
goto :eof

:shell_frontend
podman exec -it travel-frontend /bin/sh
goto :eof

:test
echo Running tests...
podman exec travel-backend python -m pytest
goto :eof

:eof
