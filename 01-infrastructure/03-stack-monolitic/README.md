# Stack Monolitic Chart

MySQL과 애플리케이션을 함께 배포하고 Ingress를 포함하는 통합 Helm 차트입니다.

## 사전 요구사항

- Kubernetes 1.19+
- Helm 3.0+
- Ingress Controller (nginx, traefik 등)
- Docker 이미지 (로컬 또는 레지스트리)

## 의존성 업데이트

차트를 설치하기 전에 의존성을 업데이트해야 합니다:

```bash
cd 01-infrastructure/03-stack-monolitic
helm dependency update
```

## 설치

### 1. Docker 이미지 준비

애플리케이션 이미지를 준비합니다:

**로컬 개발 (minikube, kind, k3d):**
```bash
cd 02-backend/00-monolitic
docker build -t 00-monolitic:latest .

# minikube
eval $(minikube docker-env)
docker build -t 00-monolitic:latest 02-backend/00-monolitic

# kind
kind load docker-image 00-monolitic:latest --name <cluster-name>

# k3d
k3d image import 00-monolitic:latest -c <cluster-name>
```

**프로덕션 (이미지 레지스트리):**
```bash
docker tag 00-monolitic:latest your-registry.com/namespace/00-monolitic:latest
docker push your-registry.com/namespace/00-monolitic:latest
```

### 2. Helm 차트 설치

기본값으로 설치 (localhost로 설정됨):

```bash
cd 01-infrastructure/03-stack-monolitic
helm dependency update
helm install monolitic-stack .
```

프로덕션 배포 시 도메인 변경:

```bash
helm install monolitic-stack . \
  --set ingress.hosts[0].host=app.example.com \
  --set app-monolitic.image.repository=your-registry.com/namespace/00-monolitic
```

또는 values 파일 사용:

```bash
helm install monolitic-stack . -f custom-values.yaml
```

## 구성 요소

이 Stack 차트는 다음을 배포합니다:

1. **MySQL** (`01-mysql-mono`)
   - 데이터베이스 서버
   - PersistentVolumeClaim으로 데이터 영속성

2. **Application** (`02-app-monolitic`)
   - Spring Boot 애플리케이션
   - MySQL에 연결

3. **Ingress**
   - 외부 접근을 위한 라우팅
   - 기본값: `localhost` (로컬 테스트용)
   - TLS 지원 (선택)

## 설정

### Ingress 설정

Ingress는 외부에서 애플리케이션에 접근하기 위한 도메인/경로를 설정합니다.

**기본값 (로컬 테스트):**
```yaml
ingress:
  enabled: true
  className: "nginx"
  hosts:
    - host: localhost  # 로컬 테스트용
      paths:
        - path: /
          pathType: Prefix
```

**프로덕션 배포 시:**
```yaml
ingress:
  enabled: true
  className: "nginx"
  annotations:
    # nginx.ingress.kubernetes.io/rewrite-target: /
  hosts:
    - host: app.example.com  # 실제 도메인으로 변경
      paths:
        - path: /
          pathType: Prefix
  tls:  # HTTPS를 위한 TLS 설정
    - secretName: app-tls
      hosts:
        - app.example.com
```

### MySQL 설정

```yaml
mysql:
  enabled: true
  mysql:
    rootPassword: your-root-password
    database: message_db
    user: message_user
    password: message_password
```

### 애플리케이션 설정

```yaml
app-monolitic:
  enabled: true
  image:
    repository: 00-monolitic
    tag: "latest"
    pullPolicy: IfNotPresent
  database:
    host: mysql  # MySQL 서비스 이름
    name: message_db
```

## 업그레이드

```bash
helm dependency update
helm upgrade monolitic-stack .
```

## 제거

```bash
helm uninstall monolitic-stack
```

## 확인

모든 리소스 확인:

```bash
# Pods
kubectl get pods -l app.kubernetes.io/managed-by=Helm

# Services
kubectl get svc

# Ingress
kubectl get ingress

# MySQL 연결 확인
kubectl get pods -l app.kubernetes.io/name=mysql

# Application 연결 확인
kubectl get pods -l app.kubernetes.io/name=app-monolitic
```

## 접근

### Ingress를 통한 접근

**로컬 테스트 (localhost):**
```bash
# Ingress 주소 확인
kubectl get ingress

# Port forward로 접근
kubectl port-forward svc/<release-name>-app-monolitic 8080:8080
# 또는
kubectl port-forward ingress/<release-name>-stack-monolitic-ingress 8080:80

# 브라우저에서 http://localhost:8080 접근
```

**프로덕션 (실제 도메인):**
```bash
# Ingress 주소 확인
kubectl get ingress

# 도메인이 DNS에 설정되어 있어야 함
# 브라우저에서 http://app.example.com 접근
```

### Port Forward를 통한 접근

```bash
# 애플리케이션
kubectl port-forward svc/<release-name>-app-monolitic 8080:8080

# MySQL (선택)
kubectl port-forward svc/<release-name>-mysql 3306:3306
```

## 트러블슈팅

### 의존성 업데이트 오류

```bash
# charts/ 폴더 삭제 후 재업데이트
rm -rf charts/
helm dependency update
```

### 이미지를 찾을 수 없음

로컬 이미지 사용 시:
- `pullPolicy: IfNotPresent` 또는 `Never`로 설정
- 클러스터에 이미지가 로드되었는지 확인

### MySQL 연결 실패

- MySQL 서비스 이름이 `mysql`인지 확인
- MySQL Pod가 실행 중인지 확인
- 데이터베이스 인증 정보 확인

### localhost Ingress 접근 문제

일부 Ingress Controller는 localhost를 지원하지 않을 수 있습니다. 이 경우:
- Port forward 사용: `kubectl port-forward svc/<service-name> 8080:8080`
- 또는 실제 도메인 사용: `--set ingress.hosts[0].host=app.local` (로컬 DNS 설정 필요)

