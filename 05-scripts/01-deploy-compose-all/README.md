# Docker Compose 배포 스크립트

로컬 통합 환경을 Docker Compose로 실행하기 위한 스크립트입니다.

## 구조

```
01-deploy-compose-all/
├── .env                      # 개인 설정 파일 (Git ignore, 실제 사용)
├── docker-compose.sh         # Docker Compose 실행 스크립트 (Linux/Mac)
├── docker-compose.ps1        # Docker Compose 실행 스크립트 (Windows)
└── README.md
```

> **참고**: Docker Compose 설정은 `01-infrastructure/00-compose-all`에 위치합니다.  
> 스크립트는 자동으로 해당 디렉토리를 참조하며, 이 디렉토리의 `.env` 파일을 사용합니다.

## Prerequisites

배포를 시작하기 전에 다음 요구사항을 확인하세요.

### 필수 도구

- Docker
- Docker Compose

### 초기 설정

**.env 파일 설정:**

```bash
# default.env를 복사하여 개인 설정 파일 생성
cp ../../01-infrastructure/00-compose-all/default.env .env
```

```powershell
# Windows
Copy-Item ..\..\01-infrastructure\00-compose-all\default.env .env
```

`.env` 파일에서 필요한 설정을 수정하세요.

## 사용법

### 스크립트 사용법

```bash
# Linux/Mac
./docker-compose.sh           # 기본값: up
./docker-compose.sh <action> [service]

# 도움말
./docker-compose.sh --help
```

```powershell
# Windows
.\docker-compose.ps1           # 기본값: up
.\docker-compose.ps1 <action> [service]

# 도움말
.\docker-compose.ps1 -Help
```

### 액션 종류

| 액션 | 설명 |
|------|------|
| `up` | 모든 서비스 시작 (기본값) |
| `down` | 모든 서비스 중지 |
| `ps` | 서비스 상태 확인 |
| `logs [service]` | 로그 보기 (전체 또는 특정 서비스) |
| `restart [service]` | 서비스 재시작 (전체 또는 특정 서비스) |

### 예제

#### 1. 서비스 시작

```bash
./docker-compose.sh up
# 또는
./docker-compose.sh
```

```powershell
.\docker-compose.ps1 up
# 또는
.\docker-compose.ps1
```

#### 2. 서비스 중지

```bash
./docker-compose.sh down
```

```powershell
.\docker-compose.ps1 down
```

#### 3. 상태 확인

```bash
./docker-compose.sh ps
```

```powershell
.\docker-compose.ps1 ps
```

#### 4. 로그 보기

```bash
# 모든 서비스 로그
./docker-compose.sh logs

# 특정 서비스 로그
./docker-compose.sh logs mysql
./docker-compose.sh logs nginx
```

```powershell
# 모든 서비스 로그
.\docker-compose.ps1 logs

# 특정 서비스 로그
.\docker-compose.ps1 logs mysql
.\docker-compose.ps1 logs nginx
```

#### 5. 서비스 재시작

```bash
# 모든 서비스 재시작
./docker-compose.sh restart

# 특정 서비스 재시작
./docker-compose.sh restart nginx
```

```powershell
# 모든 서비스 재시작
.\docker-compose.ps1 restart

# 특정 서비스 재시작
.\docker-compose.ps1 restart nginx
```
