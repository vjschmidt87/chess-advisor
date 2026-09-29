# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration=production

# Runtime stage
FROM nginx:alpine
RUN apk add --no-cache curl
COPY .nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/chess-advisor-frontend/browser /usr/share/nginx/html
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /usr/share/nginx/html && \
    chown -R appuser:appgroup /var/cache/nginx && \
    chown -R appuser:appgroup /var/log/nginx && \
    touch /var/run/nginx.pid && chown -R appuser:appgroup /var/run/nginx.pid
USER appuser
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://127.0.0.1/health || exit 1
CMD ["nginx", "-g", "daemon off;"]
