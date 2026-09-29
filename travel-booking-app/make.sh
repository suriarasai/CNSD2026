#!/bin/sh
set -e

# Prefer a provider installed in this shell's environment over a Windows
# docker-compose binary inherited through WSL's PATH.
if [ -z "${PODMAN_COMPOSE_PROVIDER:-}" ] && command -v podman-compose >/dev/null 2>&1; then
    PODMAN_COMPOSE_PROVIDER=$(command -v podman-compose)
    export PODMAN_COMPOSE_PROVIDER
fi

# WSL installations without a systemd user bus cannot run build containers
# with Podman's default systemd cgroup manager.
if [ -z "${CONTAINERS_CONF_OVERRIDE:-}" ] && [ -r /proc/sys/kernel/osrelease ] &&
    grep -qi microsoft /proc/sys/kernel/osrelease && [ ! -S "/run/user/$(id -u)/bus" ]; then
    CONTAINERS_CONF_OVERRIDE="$PWD/config/containers-wsl.conf"
    export CONTAINERS_CONF_OVERRIDE
fi

usage() {
    cat <<'EOF'
Travel Booking Application - Development Commands

Usage: ./make.sh [command]

Commands:
  init            - Initialize project directories
  build           - Build all containers
  up              - Start all services
  down            - Stop all services
  restart         - Restart all services
  logs            - Show logs for all services
  status          - Show status of all containers
  clean           - Remove containers, volumes, and local images
  shell-backend   - Open shell in backend container
  shell-frontend  - Open shell in frontend container
  test            - Run backend tests
  help            - Show this help message
EOF
}

command=${1:-help}
if [ "$#" -gt 0 ]; then
    shift
fi

# Keep all container commands in the same Podman store when WSL has no
# systemd user bus. sudo may prompt for the user's password.
case "$command" in
    build|up|down|restart|logs|status|clean|shell-backend|shell-frontend|test)
        if [ "$(id -u)" -ne 0 ] && [ -r /proc/sys/kernel/osrelease ] &&
            grep -qi microsoft /proc/sys/kernel/osrelease && [ ! -S "/run/user/$(id -u)/bus" ]; then
            echo 'WSL user bus unavailable; using Ubuntu rootful Podman via sudo.' >&2
            exec sudo -- "$0" "$command" "$@"
        fi
        ;;
esac

case "$command" in
    init)
        mkdir -p logs config monitoring
        echo 'Project initialized!'
        ;;
    build)
        exec podman compose build
        ;;
    up)
        podman compose up -d
        echo 'Services started!'
        echo 'Frontend: http://localhost:8080'
        echo 'Backend API: http://localhost:8080/api'
        echo 'Monitoring: http://localhost:8081'
        ;;
    down)
        exec podman compose down
        ;;
    restart)
        exec podman compose restart
        ;;
    logs)
        exec podman compose logs -f "$@"
        ;;
    status)
        exec podman ps -a --filter 'name=travel-'
        ;;
    clean)
        exec podman compose down -v --rmi local
        ;;
    shell-backend)
        exec podman exec -it travel-backend /bin/sh
        ;;
    shell-frontend)
        exec podman exec -it travel-frontend /bin/sh
        ;;
    test)
        exec podman exec travel-backend python -m pytest
        ;;
    help)
        usage
        ;;
    *)
        usage >&2
        exit 1
        ;;
esac
