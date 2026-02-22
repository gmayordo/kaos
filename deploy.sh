#!/bin/bash
# deploy.sh — Despliegue rápido de KAOS en Docker
# Uso: ./deploy.sh [--no-cache] [--backend-only] [--frontend-only]
set -e

# ─── Colores ───────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

log()  { echo -e "${CYAN}[KAOS]${NC} $1"; }
ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()  { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ─── Directorio del script ─────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ─── Args ─────────────────────────────────────────────────────────────────────
NO_CACHE=""
BUILD_TARGET="all"
for arg in "$@"; do
  case $arg in
    --no-cache)      NO_CACHE="--no-cache" ;;
    --backend-only)  BUILD_TARGET="backend" ;;
    --frontend-only) BUILD_TARGET="frontend" ;;
  esac
done

# ─── Checks previos ────────────────────────────────────────────────────────────
log "Verificando entorno..."

[ -f ".env" ] || err ".env no encontrado. Copia .env.example a .env y configura los valores."

docker network inspect postgres_ehcos-network &>/dev/null \
  || err "La red 'postgres_ehcos-network' no existe. ¿Está levantado el stack de postgres en dockerconf?"

# Verificar que postgres-container responde
docker exec postgres-container pg_isready &>/dev/null \
  || warn "postgres-container no responde al pg_isready, revisa que la base de datos esté corriendo."

ok "Red y base de datos disponibles"

# ─── Build y deploy ────────────────────────────────────────────────────────────
echo ""
log "Inicio de despliegue — target: ${BUILD_TARGET} ${NO_CACHE:+(--no-cache)}"
echo ""

case $BUILD_TARGET in
  backend)
    log "Construyendo imagen backend..."
    docker compose build $NO_CACHE backend

    log "Reiniciando contenedor backend..."
    docker compose up -d --force-recreate backend
    ;;

  frontend)
    log "Construyendo imagen frontend..."
    docker compose build $NO_CACHE frontend

    log "Reiniciando contenedor frontend..."
    docker compose up -d --force-recreate frontend
    ;;

  all)
    log "Construyendo imágenes (backend + frontend en paralelo)..."
    docker compose build $NO_CACHE

    log "Levantando servicios..."
    docker compose up -d --force-recreate
    ;;
esac

# ─── Esperar health checks ─────────────────────────────────────────────────────
echo ""
log "Esperando health checks..."

wait_healthy() {
  local name=$1
  local max=30
  local i=0
  while [ $i -lt $max ]; do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "no-health")
    if [ "$status" = "healthy" ] || [ "$status" = "no-health" ]; then
      ok "$name → ${status}"
      return 0
    fi
    echo -n "."
    sleep 2
    i=$((i+1))
  done
  warn "$name tardó más de $((max*2))s en estar healthy (status: $status)"
}

[ "$BUILD_TARGET" != "frontend" ] && wait_healthy "kaos-backend"
[ "$BUILD_TARGET" != "backend"  ] && wait_healthy "kaos-frontend"

# ─── Resumen ───────────────────────────────────────────────────────────────────
FRONTEND_PORT=$(grep FRONTEND_PORT .env | cut -d= -f2 | tr -d ' ')
BACKEND_PORT=$(grep BACKEND_PORT .env | cut -d= -f2 | tr -d ' ')
FRONTEND_PORT=${FRONTEND_PORT:-2000}
BACKEND_PORT=${BACKEND_PORT:-6060}

echo ""
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}  KAOS desplegado correctamente ✓${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"
[ "$BUILD_TARGET" != "backend"  ] && echo -e "  Frontend:  ${CYAN}http://localhost:${FRONTEND_PORT}${NC}"
[ "$BUILD_TARGET" != "frontend" ] && echo -e "  Backend:   ${CYAN}http://localhost:${BACKEND_PORT}${NC}"
[ "$BUILD_TARGET" != "frontend" ] && echo -e "  Swagger:   ${CYAN}http://localhost:${BACKEND_PORT}/swagger-ui.html${NC}"
echo ""
echo -e "  Logs:   ${YELLOW}docker compose logs -f${NC}"
echo -e "  Stop:   ${YELLOW}docker compose down${NC}"
echo ""
