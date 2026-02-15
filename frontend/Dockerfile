# ---- Build Stage ----
FROM node:20-alpine AS builder
WORKDIR /app

# Instalar dependencias primero (cache layer)
COPY package.json package-lock.json* ./
RUN npm ci

# Copiar código fuente y compilar
COPY . .
RUN npm run build

# ---- Production Stage ----
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

# Copiar config de nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar build de Vite
COPY --from=builder /app/dist .

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
