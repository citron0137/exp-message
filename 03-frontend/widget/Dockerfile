# =============================================================================
# Multi-stage Dockerfile for the exp-message widget
# Builds a React/Vite application and serves it with Nginx
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build the application
# -----------------------------------------------------------------------------
FROM node:22-alpine AS builder

# Set working directory
WORKDIR /app

# Copy package files for dependency installation
COPY package.json package-lock.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application
# Vite will output to /app/dist
RUN npm run build

# -----------------------------------------------------------------------------
# Stage 2: Serve the built application with Nginx
# -----------------------------------------------------------------------------
FROM nginx:alpine

# Install envsubst for environment variable substitution
RUN apk add --no-cache gettext

# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy entrypoint script for runtime configuration
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Copy custom index.html for serving the widget in browser
COPY widget-index.html /usr/share/nginx/html/index.html

# Expose the port that Nginx will listen on
EXPOSE 80

# Use custom entrypoint to handle environment variables at runtime
ENTRYPOINT ["/docker-entrypoint.sh"]

# Start Nginx in foreground mode
CMD ["nginx", "-g", "daemon off;"]
