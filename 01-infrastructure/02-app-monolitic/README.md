# Monolithic Application Helm Chart

Spring Boot 애플리케이션을 Kubernetes에 배포하기 위한 Helm 차트입니다.

## 사전 요구사항

- Kubernetes 1.19+
- Helm 3.0+
- MySQL 서비스 (01-mysql-mono 차트로 배포 가능)

## 설치

### 1. Docker 이미지 빌드

먼저 애플리케이션 Docker 이미지를 빌드해야 합니다:

```bash
cd 02-backend/00-monolitic
docker build -t 00-monolitic:latest .
```

### 2. 이미지 사용 방법

#### 옵션 A: 로컬 이미지 사용 (개발 환경)

**minikube 사용 시:**
```bash
# minikube의 Docker 데몬 사용
eval $(minikube docker-env)
docker build -t 00-monolitic:latest 02-backend/00-monolitic
```

**kind 사용 시:**
```bash
# kind 클러스터에 이미지 로드
kind load docker-image 00-monolitic:latest --name <cluster-name>
```

**k3d 사용 시:**
```bash
# k3d 클러스터에 이미지 로드
k3d image import 00-monolitic:latest -c <cluster-name>
```

로컬 이미지 사용 시 `imagePullPolicy: IfNotPresent` 또는 `Never`로 설정:
```bash
helm install app-monolitic ./01-infrastructure/02-app-monolitic \
  --set image.pullPolicy=IfNotPresent
```

#### 옵션 B: 이미지 레지스트리 사용 (프로덕션)

이미지 레지스트리에 푸시:

```bash
docker tag 00-monolitic:latest your-registry.com/namespace/00-monolitic:latest
docker push your-registry.com/namespace/00-monolitic:latest
```

레지스트리 사용 시 values.yaml 설정:
```yaml
image:
  registry: "your-registry.com"
  repository: "namespace/00-monolitic"
  tag: "latest"
  pullPolicy: Always
```

### 3. Helm 차트 설치

기본값으로 설치:

```bash
helm install app-monolitic ./01-infrastructure/02-app-monolitic
```

커스텀 값으로 설치:

```bash
helm install app-monolitic ./01-infrastructure/02-app-monolitic \
  --set image.repository=your-registry.com/namespace/00-monolitic \
  --set database.host=mysql-service-name \
  --set database.name=your_db_name
```

또는 values 파일 사용:

```bash
helm install app-monolitic ./01-infrastructure/02-app-monolitic -f custom-values.yaml
```

## 설정

주요 설정 값들:

### 이미지 설정

**로컬 이미지 사용 (개발):**
```yaml
image:
  repository: 00-monolitic
  tag: "latest"
  pullPolicy: IfNotPresent  # 또는 Never
```

**이미지 레지스트리 사용 (프로덕션):**
```yaml
image:
  registry: "your-registry.com"
  repository: "namespace/00-monolitic"
  tag: "latest"
  pullPolicy: Always
```

### 데이터베이스 설정

```yaml
database:
  host: mysql  # MySQL 서비스 이름
  port: 3306
  name: message_db
  username: message_user
  password: message_password
```

### 리소스 설정

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

### 서비스 설정

```yaml
service:
  type: ClusterIP  # ClusterIP, NodePort, LoadBalancer
  port: 8080
```

## 업그레이드

```bash
helm upgrade app-monolitic ./01-infrastructure/02-app-monolitic
```

## 제거

```bash
helm uninstall app-monolitic
```

## 확인

배포 상태 확인:

```bash
kubectl get pods -l app.kubernetes.io/name=app-monolitic
```

서비스 확인:

```bash
kubectl get svc -l app.kubernetes.io/name=app-monolitic
```

로그 확인:

```bash
kubectl logs -l app.kubernetes.io/name=app-monolitic
```

## Health Check

애플리케이션은 Spring Boot Actuator의 `/actuator/health` 엔드포인트를 사용합니다.

Health Check 확인:

```bash
kubectl port-forward svc/app-monolitic 8080:8080
curl http://localhost:8080/actuator/health
```

