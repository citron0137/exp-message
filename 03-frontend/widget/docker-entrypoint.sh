#!/bin/sh
# =============================================================================
# Docker entrypoint script for the widget frontend
# Handles runtime environment variable substitution in HTML/JS files
# =============================================================================

set -e

# -----------------------------------------------------------------------------
# Substitute environment variables into the built files
# This allows runtime configuration of the widget
# -----------------------------------------------------------------------------

# Replace API base URL placeholder with actual value
sed -i "s|__WIDGET_API_BASE_URL__|${WIDGET_API_BASE_URL:-http://localhost:80/api}|g" /usr/share/nginx/html/assets/*.js 2>/dev/null || true
sed -i "s|__WIDGET_API_BASE_URL__|${WIDGET_API_BASE_URL:-http://localhost:80/api}|g" /usr/share/nginx/html/*.js 2>/dev/null || true
sed -i "s|__WIDGET_API_BASE_URL__|${WIDGET_API_BASE_URL:-http://localhost:80/api}|g" /usr/share/nginx/html/index.html 2>/dev/null || true

# Replace WebSocket URL placeholder with actual value
sed -i "s|__WIDGET_API_WEBSOCKET_URL__|${WIDGET_API_WEBSOCKET_URL:-ws://localhost:80/ws}|g" /usr/share/nginx/html/assets/*.js 2>/dev/null || true
sed -i "s|__WIDGET_API_WEBSOCKET_URL__|${WIDGET_API_WEBSOCKET_URL:-ws://localhost:80/ws}|g" /usr/share/nginx/html/*.js 2>/dev/null || true
sed -i "s|__WIDGET_API_WEBSOCKET_URL__|${WIDGET_API_WEBSOCKET_URL:-ws://localhost:80/ws}|g" /usr/share/nginx/html/index.html 2>/dev/null || true

# Replace debug mode placeholder
sed -i "s|__WIDGET_DEBUG_MODE__|${WIDGET_DEBUG_MODE:-false}|g" /usr/share/nginx/html/assets/*.js 2>/dev/null || true
sed -i "s|__WIDGET_DEBUG_MODE__|${WIDGET_DEBUG_MODE:-false}|g" /usr/share/nginx/html/*.js 2>/dev/null || true
sed -i "s|__WIDGET_DEBUG_MODE__|${WIDGET_DEBUG_MODE:-false}|g" /usr/share/nginx/html/index.html 2>/dev/null || true

# Replace container ID placeholder
sed -i "s|__WIDGET_CONTAINER_ID__|${WIDGET_CONTAINER_ID:-exp-message-widget-root}|g" /usr/share/nginx/html/assets/*.js 2>/dev/null || true
sed -i "s|__WIDGET_CONTAINER_ID__|${WIDGET_CONTAINER_ID:-exp-message-widget-root}|g" /usr/share/nginx/html/*.js 2>/dev/null || true
sed -i "s|__WIDGET_CONTAINER_ID__|${WIDGET_CONTAINER_ID:-exp-message-widget-root}|g" /usr/share/nginx/html/index.html 2>/dev/null || true

# Execute the provided command (nginx)
exec "$@"
