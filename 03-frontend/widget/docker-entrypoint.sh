#!/bin/sh
# =============================================================================
# Docker entrypoint script for the widget frontend
# Generates runtime configuration file (config.js)
# =============================================================================

set -e

# -----------------------------------------------------------------------------
# Generate config.js with runtime environment variables
# This allows runtime configuration without modifying built files
# -----------------------------------------------------------------------------

cat > /usr/share/nginx/html/config.js << EOF
// Runtime configuration injected by Docker
window.__WIDGET_CONFIG__ = {
  apiUrl: "${WIDGET_API_BASE_URL:-http://localhost:80}",
  wsUrl: "${WIDGET_API_WEBSOCKET_URL:-ws://localhost:80/api/ws}",
  timeout: ${WIDGET_API_TIMEOUT_MS:-10000},
  containerId: "${WIDGET_CONTAINER_ID:-exp-message-widget-root}",
  debug: ${WIDGET_DEBUG_MODE:-false}
};
EOF

echo "Generated config.js with runtime configuration"

# Execute the provided command (nginx)
exec "$@"
