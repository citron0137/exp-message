#!/bin/bash
# =============================================================================
# Helm Chart Deployment Script
# Supports install, upgrade, uninstall actions.
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VALUES_FILE="${SCRIPT_DIR}/values.yaml"
KUBECONFIG_FILE="${SCRIPT_DIR}/kubeconfig.yaml"
RELEASE_NAME="message-stack"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Log functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Prerequisites check
pre_check() {
    # Check values.yaml exists
    if [ ! -f "$VALUES_FILE" ]; then
        log_error "values.yaml not found: $VALUES_FILE"
        log_info "Copy values.yaml.example to values.yaml first."
        exit 1
    fi
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Config file: $VALUES_FILE"
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Release name: $RELEASE_NAME"
}

# Install (create)
do_install() {
    log_info "=========================================="
    log_info "Helm Install Started"
    log_info "=========================================="
    
    pre_check
    
    log_info "Updating dependencies..."
    helm dependency update --kubeconfig="$KUBECONFIG_FILE" "$SCRIPT_DIR"
    
    log_info "Running Helm Install..."
    helm install "$RELEASE_NAME" "$SCRIPT_DIR" \
        --kubeconfig="$KUBECONFIG_FILE" \
        --values "$VALUES_FILE"
    
    log_info "Checking deployment status..."
    kubectl get pods --kubeconfig="$KUBECONFIG_FILE" \
        -l app.kubernetes.io/instance="$RELEASE_NAME"
    
    log_success "=========================================="
    log_success "Helm Install Completed!"
    log_success "=========================================="
}

# Upgrade (with rollout restart)
do_upgrade() {
    log_info "=========================================="
    log_info "Helm Upgrade Started"
    log_info "=========================================="
    
    pre_check
    
    log_info "Updating dependencies..."
    helm dependency update --kubeconfig="$KUBECONFIG_FILE" "$SCRIPT_DIR"
    
    log_info "Running Helm Upgrade..."
    helm upgrade "$RELEASE_NAME" "$SCRIPT_DIR" \
        --kubeconfig="$KUBECONFIG_FILE" \
        --values "$VALUES_FILE"
    
    log_info "Restarting pod rollout (required for latest tag)..."
    kubectl rollout restart deployment/"${RELEASE_NAME}-app-monolitic" \
        --kubeconfig="$KUBECONFIG_FILE" 2>/dev/null || \
        log_warn "app-monolitic deployment rollout failed (may not exist)"
    
    log_info "Checking deployment status..."
    kubectl get pods --kubeconfig="$KUBECONFIG_FILE" \
        -l app.kubernetes.io/instance="$RELEASE_NAME"
    
    log_success "=========================================="
    log_success "Helm Upgrade Completed!"
    log_success "=========================================="
}

# Logs - App
do_logs_app() {
    log_info "=========================================="
    log_info "App (00-monolitic) Logs"
    log_info "=========================================="
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Press Ctrl+C to exit"
    echo ""
    
    kubectl logs -f -l app.kubernetes.io/name=app-monolitic \
        --kubeconfig="$KUBECONFIG_FILE" \
        --tail=100
}

# Logs - Migration
do_logs_migration() {
    log_info "=========================================="
    log_info "Migration (01-db-migrations) Logs"
    log_info "=========================================="
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Press Ctrl+C to exit"
    echo ""
    
    kubectl logs -f -l app.kubernetes.io/name=batch-db-migration \
        --kubeconfig="$KUBECONFIG_FILE" \
        --tail=100
}

# Uninstall (delete)
do_uninstall() {
    log_info "=========================================="
    log_info "Helm Uninstall Started"
    log_info "=========================================="
    
    # Check kubeconfig.yaml exists
    if [ ! -f "$KUBECONFIG_FILE" ]; then
        log_error "kubeconfig.yaml not found: $KUBECONFIG_FILE"
        exit 1
    fi
    
    log_info "Kubeconfig: $KUBECONFIG_FILE"
    log_info "Release name: $RELEASE_NAME"
    
    log_warn "This will delete release '$RELEASE_NAME'. Continue? (y/N)"
    read -r confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        log_info "Cancelled."
        exit 0
    fi
    
    log_info "Running Helm Uninstall..."
    helm uninstall "$RELEASE_NAME" --kubeconfig="$KUBECONFIG_FILE"
    
    log_success "=========================================="
    log_success "Helm Uninstall Completed!"
    log_success "=========================================="
}

# Help
show_help() {
    echo "Usage: $0 [action]"
    echo ""
    echo "Options:"
    echo "  -h, --help       Show this help message"
    echo ""
    echo "Actions:"
    echo "  install, c       Install new release"
    echo "  upgrade, u       Upgrade existing release (with pod rollout) [default]"
    echo "  uninstall, d     Delete release"
    echo "  logs-app, la     View App (00-monolitic) logs"
    echo "  logs-migration, lm  View Migration (01-db-migrations) logs"
    echo ""
    echo "Examples:"
    echo "  $0           # Upgrade (default)"
    echo "  $0 c         # Install"
    echo "  $0 u         # Upgrade"
    echo "  $0 d         # Uninstall"
    echo "  $0 la        # App logs"
    echo "  $0 lm        # Migration logs"
}

# Main
ACTION=""

# Parse arguments
if [[ $# -gt 0 ]]; then
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            ACTION="$1"
            ;;
    esac
fi

if [ -z "$ACTION" ]; then
    log_info "No action specified. Using default 'upgrade'."
    ACTION="upgrade"
fi

case $ACTION in
    install|c)
        do_install
        ;;
    upgrade|u)
        do_upgrade
        ;;
    uninstall|d)
        do_uninstall
        ;;
    logs-app|la)
        do_logs_app
        ;;
    logs-migration|lm)
        do_logs_migration
        ;;
    *)
        log_error "Unknown action: $ACTION"
        show_help
        exit 1
        ;;
esac
