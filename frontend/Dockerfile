# ---------- Stage 1: build ----------
# Node build stage compiles the React app and emits static files
FROM node:18-alpine AS build

WORKDIR /app

# Install dependencies first for layer caching
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

COPY . .

# API URL is injected at build time; see README
ARG REACT_APP_API_URL=http://localhost:8080/api
ENV REACT_APP_API_URL=$REACT_APP_API_URL

RUN npm run build

# ---------- Stage 2: runtime ----------
# nginx:alpine serves the static build output only - no node_modules
# in the final image, which keeps the image small
FROM nginx:1.27-alpine

# Replace default config with one that also proxies /api to the backend
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -q --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
