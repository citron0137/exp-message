# Redis Helm Chart

Redis 캐시/메시지 브로커를 Kubernetes에 배포하기 위한 Helm 차트입니다.

## 사전 요구사항

- Kubernetes 1.19+
- Helm 3.0+

## 설치

기본값으로 설치:

```bash
helm install redis ./01-infrastructure/03-redis-mono
```

커스텀 값으로 설치:

```bash
helm install redis ./01-infrastructure/03-redis-mono \
  --set redis.password=your-secure-password \
  --set persistence.size=10Gi
```

또는 values 파일 사용:

```bash
helm install redis ./01-infrastructure/03-redis-mono -f custom-values.yaml
```

## 설정

주요 설정 값들:

### Redis 설정

```yaml
redis:
  port: 6379
  password: redispassword
  maxmemory: 256mb
  maxmemoryPolicy: allkeys-lru
```

### 영속성 설정

```yaml
persistence:
  enabled: true
  type: "storageClass"
  storageClass: ""
  accessMode: ReadWriteOnce
  size: 5Gi
```

### Redis 고급 설정

```yaml
config:
  # AOF (Append Only File) 영속성
  appendonly: "yes"
  appendfsync: "everysec"
  
  # RDB 스냅샷 영속성
  save: "900 1 300 10 60 10000"
  
  # 메모리 관리 정책
  maxmemoryPolicy: "allkeys-lru"
  
  # 네트워크
  timeout: 300
  tcpKeepalive: 60
  
  # 로깅
  loglevel: "notice"
```

### 리소스 설정

```yaml
resources:
  requests:
    memory: "128Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

### 서비스 설정

```yaml
service:
  type: ClusterIP  # ClusterIP, NodePort, LoadBalancer
  port: 6379
  nodePort: null  # NodePort 타입일 때 사용
```

## 업그레이드

```bash
helm upgrade redis ./01-infrastructure/03-redis-mono
```

## 제거

```bash
helm uninstall redis
```

## 확인

배포 상태 확인:

```bash
kubectl get pods -l app.kubernetes.io/name=redis
```

서비스 확인:

```bash
kubectl get svc -l app.kubernetes.io/name=redis
```

로그 확인:

```bash
kubectl logs -l app.kubernetes.io/name=redis
```

## Redis 클라이언트 연결

### 포트 포워딩을 통한 연결

```bash
kubectl port-forward svc/redis 6379:6379
```

다른 터미널에서:

```bash
redis-cli -h localhost -p 6379 -a redispassword
```

### Pod 내에서 직접 연결

```bash
kubectl exec -it <redis-pod-name> -- redis-cli -a redispassword
```

## 영속성

Redis는 AOF (Append Only File)와 RDB 스냅샷을 모두 지원합니다:

- **AOF**: 모든 쓰기 작업을 로그 파일에 기록 (더 안전하지만 느림)
- **RDB**: 주기적으로 메모리 스냅샷 저장 (더 빠르지만 최신 데이터 손실 가능)

기본 설정은 AOF를 활성화하고 있습니다.

## 메모리 관리

기본 메모리 제한은 256MB이며, `maxmemory-policy`는 `allkeys-lru`로 설정되어 있습니다.

다른 정책 옵션:
- `noeviction`: 메모리 부족 시 에러 반환
- `allkeys-lru`: 가장 오래 사용되지 않은 키 제거
- `volatile-lru`: 만료 시간이 있는 키 중 LRU 제거
- `allkeys-random`: 랜덤 키 제거
- `volatile-random`: 만료 시간이 있는 키 중 랜덤 제거
- `volatile-ttl`: 가장 짧은 TTL을 가진 키 제거

## 보안

기본적으로 Redis는 비밀번호 인증을 사용합니다. 프로덕션 환경에서는 반드시 강력한 비밀번호를 설정하세요:

```bash
helm install redis ./01-infrastructure/03-redis-mono \
  --set redis.password=$(openssl rand -base64 32)
```
