@echo off
echo === Testing Redis Caching ===
echo.

echo [1] Clear any existing cache...
podman exec travel-redis redis-cli FLUSHDB
echo.

echo [2] First request (should be CACHE MISS)...
podman exec travel-backend python -c "import urllib.request; urllib.request.urlopen('http://localhost:5000/api/bookings').read()" > nul
podman logs travel-backend --tail 3 | findstr "Cache"
echo.

echo [3] Second request (should be CACHE HIT)...
timeout /t 2 > nul
podman exec travel-backend python -c "import urllib.request; urllib.request.urlopen('http://localhost:5000/api/bookings').read()" > nul
podman logs travel-backend --tail 3 | findstr "Cache"
echo.

echo [4] Check cached data in Redis...
podman exec travel-redis redis-cli KEYS "*"
echo.

echo [5] Check Redis cache statistics...
podman exec travel-redis redis-cli INFO stats
echo.
echo.

echo === Test Complete ===
pause
