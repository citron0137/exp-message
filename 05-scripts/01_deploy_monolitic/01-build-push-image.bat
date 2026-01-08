@echo off
REM Monolitic server build and push script (Windows)

setlocal enabledelayedexpansion

REM Script directory
set SCRIPT_DIR=%~dp0

REM Load .env file if exists
if exist "%SCRIPT_DIR%.env" (
    echo Loading environment file: %SCRIPT_DIR%.env
    for /f "usebackq eol=# tokens=1* delims==" %%a in ("%SCRIPT_DIR%.env") do (
        set "key=%%a"
        set "value=%%b"
        REM Remove leading/trailing spaces
        for /f "tokens=*" %%k in ("!key!") do set "key=%%k"
        for /f "tokens=*" %%v in ("!value!") do set "value=%%v"
        REM Set variable if key is not empty
        if not "!key!"=="" (
            set "!key!=!value!"
            echo   Loaded: !key!=!value!
        )
    )
) else if exist "%SCRIPT_DIR%default.env" (
    echo Loading default environment file: %SCRIPT_DIR%default.env
    for /f "usebackq eol=# tokens=1* delims==" %%a in ("%SCRIPT_DIR%default.env") do (
        set "key=%%a"
        set "value=%%b"
        REM Remove leading/trailing spaces
        for /f "tokens=*" %%k in ("!key!") do set "key=%%k"
        for /f "tokens=*" %%v in ("!value!") do set "value=%%v"
        REM Set variable if key is not empty
        if not "!key!"=="" (
            set "!key!=!value!"
            echo   Loaded: !key!=!value!
        )
    )
)

REM Set default values (use delayed expansion)
if "!REGISTRY_HOST!"=="" set REGISTRY_HOST=localhost
if "!REGISTRY_PORT!"=="" set REGISTRY_PORT=5000
if "!IMAGE_NAME!"=="" set IMAGE_NAME=00-monolitic
if "!IMAGE_TAG!"=="" set IMAGE_TAG=latest

REM Debug: Show loaded values
echo Debug - REGISTRY_HOST: [!REGISTRY_HOST!]
echo Debug - REGISTRY_PORT: [!REGISTRY_PORT!] (length: !REGISTRY_PORT:~0,1!)
echo Debug - IMAGE_NAME: [!IMAGE_NAME!]
echo Debug - IMAGE_TAG: [!IMAGE_TAG!]

REM Project root directory
set PROJECT_ROOT=%SCRIPT_DIR%..\..
set MONOLITIC_DIR=%PROJECT_ROOT%\02-backend\00-monolitic

REM Build registry image name (포트가 없거나 0인 경우 처리)
REM Remove any trailing spaces from REGISTRY_PORT
for /f "tokens=*" %%p in ("!REGISTRY_PORT!") do set REGISTRY_PORT=%%p

if "!REGISTRY_PORT!"=="" (
    set REGISTRY_IMAGE=!REGISTRY_HOST!/!IMAGE_NAME!:!IMAGE_TAG!
    set REGISTRY_DISPLAY=!REGISTRY_HOST!
) else if "!REGISTRY_PORT!"=="0" (
    set REGISTRY_IMAGE=!REGISTRY_HOST!/!IMAGE_NAME!:!IMAGE_TAG!
    set REGISTRY_DISPLAY=!REGISTRY_HOST!
) else (
    REM Port is specified, use it
    set REGISTRY_IMAGE=!REGISTRY_HOST!:!REGISTRY_PORT!/!IMAGE_NAME!:!IMAGE_TAG!
    set REGISTRY_DISPLAY=!REGISTRY_HOST!:!REGISTRY_PORT!
)

echo ==========================================
echo Monolitic Server Build and Push
echo ==========================================
echo Registry: !REGISTRY_DISPLAY!
echo Image: !IMAGE_NAME!:!IMAGE_TAG!
echo Full Image: !REGISTRY_IMAGE!
echo Directory: %MONOLITIC_DIR%
echo ==========================================

REM Check directory
if not exist "%MONOLITIC_DIR%" (
    echo Error: Monolitic directory not found: %MONOLITIC_DIR%
    exit /b 1
)

REM Check Dockerfile
if not exist "%MONOLITIC_DIR%\Dockerfile" (
    echo Error: Dockerfile not found: %MONOLITIC_DIR%\Dockerfile
    exit /b 1
)

REM Build image
echo.
echo 1. Building Docker image...
cd /d "%MONOLITIC_DIR%"
docker build -t !IMAGE_NAME!:!IMAGE_TAG! .

REM Tag for registry
echo.
echo 2. Tagging for registry: !REGISTRY_IMAGE!
docker tag !IMAGE_NAME!:!IMAGE_TAG! !REGISTRY_IMAGE!

REM Push to registry
echo.
echo 3. Pushing to registry...
docker push !REGISTRY_IMAGE!

echo.
echo ==========================================
echo Done!
echo Image: !REGISTRY_IMAGE!
echo ==========================================

endlocal

