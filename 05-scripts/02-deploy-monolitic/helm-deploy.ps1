# =============================================================================
# Helm Chart Deployment Script (PowerShell)
# Supports install, upgrade, uninstall actions.
# =============================================================================

param(
    [Parameter(Position=0)]
    [string]$Action,
    
    [switch]$Help
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ValuesFile = Join-Path $ScriptDir "values.yaml"
$KubeconfigFile = Join-Path $ScriptDir "kubeconfig.yaml"
$ReleaseName = "message-stack"

# Colored output functions
function Write-Info { param($Message) Write-Host "[INFO] $Message" -ForegroundColor Blue }
function Write-Success { param($Message) Write-Host "[SUCCESS] $Message" -ForegroundColor Green }
function Write-Warn { param($Message) Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Write-Err { param($Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

# Prerequisites check
function Test-Prerequisites {
    # Check values.yaml exists
    if (-not (Test-Path $ValuesFile)) {
        Write-Err "values.yaml not found: $ValuesFile"
        Write-Info "Copy values.yaml.example to values.yaml first."
        exit 1
    }
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Config file: $ValuesFile"
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Release name: $ReleaseName"
}

# Install (create)
function Invoke-Install {
    Write-Info "=========================================="
    Write-Info "Helm Install Started"
    Write-Info "=========================================="
    
    Test-Prerequisites
    
    Write-Info "Updating dependencies..."
    helm dependency update --kubeconfig="$KubeconfigFile" $ScriptDir
    if ($LASTEXITCODE -ne 0) { throw "helm dependency update failed" }
    
    Write-Info "Running Helm Install..."
    helm install $ReleaseName $ScriptDir `
        --kubeconfig="$KubeconfigFile" `
        --values $ValuesFile
    if ($LASTEXITCODE -ne 0) { throw "helm install failed" }
    
    Write-Info "Checking deployment status..."
    kubectl get pods --kubeconfig="$KubeconfigFile" `
        -l "app.kubernetes.io/instance=$ReleaseName"
    
    Write-Success "=========================================="
    Write-Success "Helm Install Completed!"
    Write-Success "=========================================="
}

# Upgrade (with rollout restart)
function Invoke-Upgrade {
    Write-Info "=========================================="
    Write-Info "Helm Upgrade Started"
    Write-Info "=========================================="
    
    Test-Prerequisites
    
    Write-Info "Updating dependencies..."
    helm dependency update --kubeconfig="$KubeconfigFile" $ScriptDir
    if ($LASTEXITCODE -ne 0) { throw "helm dependency update failed" }
    
    Write-Info "Running Helm Upgrade..."
    helm upgrade $ReleaseName $ScriptDir `
        --kubeconfig="$KubeconfigFile" `
        --values $ValuesFile
    if ($LASTEXITCODE -ne 0) { throw "helm upgrade failed" }
    
    Write-Info "Restarting pod rollout (required for latest tag)..."
    try {
        kubectl rollout restart "deployment/${ReleaseName}-app-monolitic" `
            --kubeconfig="$KubeconfigFile" 2>$null
    } catch {
        Write-Warn "app-monolitic deployment rollout failed (may not exist)"
    }
    
    Write-Info "Checking deployment status..."
    kubectl get pods --kubeconfig="$KubeconfigFile" `
        -l "app.kubernetes.io/instance=$ReleaseName"
    
    Write-Success "=========================================="
    Write-Success "Helm Upgrade Completed!"
    Write-Success "=========================================="
}

# Logs - App
function Invoke-LogsApp {
    Write-Info "=========================================="
    Write-Info "App (00-monolitic) Logs"
    Write-Info "=========================================="
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Press Ctrl+C to exit"
    Write-Host ""
    
    kubectl logs -f -l app.kubernetes.io/name=app-monolitic `
        --kubeconfig="$KubeconfigFile" `
        --tail=100
}

# Logs - Migration
function Invoke-LogsMigration {
    Write-Info "=========================================="
    Write-Info "Migration (01-db-migrations) Logs"
    Write-Info "=========================================="
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Press Ctrl+C to exit"
    Write-Host ""
    
    kubectl logs -f -l app.kubernetes.io/name=batch-db-migration `
        --kubeconfig="$KubeconfigFile" `
        --tail=100
}

# Uninstall (delete)
function Invoke-Uninstall {
    Write-Info "=========================================="
    Write-Info "Helm Uninstall Started"
    Write-Info "=========================================="
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Release name: $ReleaseName"
    
    Write-Warn "This will delete release '$ReleaseName'. Continue? (y/N)"
    $confirm = Read-Host
    if ($confirm -ne "y" -and $confirm -ne "Y") {
        Write-Info "Cancelled."
        exit 0
    }
    
    Write-Info "Running Helm Uninstall..."
    helm uninstall $ReleaseName --kubeconfig="$KubeconfigFile"
    if ($LASTEXITCODE -ne 0) { throw "helm uninstall failed" }
    
    Write-Success "=========================================="
    Write-Success "Helm Uninstall Completed!"
    Write-Success "=========================================="
}

# Help
function Show-Help {
    Write-Host @"
Usage: .\helm-deploy.ps1 [action]

Options:
  -Help            Show this help message

Actions:
  install, c       Install new release
  upgrade, u       Upgrade existing release (with pod rollout) [default]
  uninstall, d     Delete release
  logs-app, la     View App (00-monolitic) logs
  logs-migration, lm  View Migration (01-db-migrations) logs

Examples:
  .\helm-deploy.ps1        # Upgrade (default)
  .\helm-deploy.ps1 c      # Install
  .\helm-deploy.ps1 u      # Upgrade
  .\helm-deploy.ps1 d      # Uninstall
  .\helm-deploy.ps1 la     # App logs
  .\helm-deploy.ps1 lm     # Migration logs
"@
}

# Main
if ($Help) {
    Show-Help
    exit 0
}

if (-not $Action) {
    Write-Info "No action specified. Using default 'upgrade'."
    $Action = "upgrade"
}

switch ($Action) {
    { $_ -in "install", "c" } { Invoke-Install }
    { $_ -in "upgrade", "u" } { Invoke-Upgrade }
    { $_ -in "uninstall", "d" } { Invoke-Uninstall }
    { $_ -in "logs-app", "la" } { Invoke-LogsApp }
    { $_ -in "logs-migration", "lm" } { Invoke-LogsMigration }
    default {
        Write-Err "Unknown action: $Action"
        Show-Help
        exit 1
    }
}
