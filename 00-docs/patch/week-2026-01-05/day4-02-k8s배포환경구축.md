# 패치노트 - 2026-01-08

## Kubernetes 배포 환경 구축 (k3s)

### k3s, Helm 설치 및 확인

**k3s 설치 (Traefik 비활성화):**

```bash
# Traefik을 비활성화하고 설치 (80/443 포트 충돌 방지)
curl -sfL https://get.k3s.io | sh -s - --disable traefik

# 현재 사용자가 sudo 없이 kubectl 사용하도록 설정
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
chmod 600 ~/.kube/config

# 확인
kubectl get nodes
```

**Helm 설치:**

```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
helm version
```

**Nginx Ingress Controller 설치:**

```bash
# Helm repository 추가
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

# Nginx Ingress Controller 설치 (NodePort: 30080/30443)
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.service.type=NodePort \
  --set controller.service.nodePorts.http=30080 \
  --set controller.service.nodePorts.https=30443

# 설치 확인
kubectl get pods -n ingress-nginx
kubectl get ingressclass
```


### Docker 레지스트리 설정

**로컬 Docker 레지스트리 실행:**

```bash
docker run -d -p 5000:5000 --name registry \
  --restart=always \
  -v /var/lib/registry:/var/lib/registry \
  registry:2
```

**k3s 레지스트리 설정 (HTTP insecure):**

```bash
# registries.yaml 생성 (외부 레지스트리 포함)
sudo mkdir -p /etc/rancher/k3s
sudo cat > /etc/rancher/k3s/registries.yaml <<EOF
mirrors:
  localhost:5000:
    endpoint:
      - "http://localhost:5000"
  10.137.0.195:5000:
    endpoint:
      - "http://10.137.0.195:5000"
configs:
  "10.137.0.195:5000":
    tls:
      insecure_skip_verify: true
  "localhost:5000":
    tls:
      insecure_skip_verify: true
EOF

# k3s 재시작
sudo systemctl restart k3s

# 확인
curl http://localhost:5000/v2/_catalog
```

### Windows Docker Desktop에서 HTTP 레지스트리 사용 설정

**로컬 개발 환경에서 HTTP 레지스트리 사용:**
```
1. Docker Desktop 열기
2. Settings (톱니바퀴 아이콘) 클릭
3. Docker Engine 메뉴 선택
4. JSON 설정에 "insecure-registries" 추가:
   {
     "insecure-registries": ["10.137.0.195:5000"]
   }
5. "Apply & Restart" 클릭
```

**중요: `.env` 파일 설정**
```
REGISTRY_HOST=10.137.0.195
REGISTRY_PORT=5000
IMAGE_NAME=00-monolitic
IMAGE_TAG=latest
```

### Ingress 접근 방법

**Ingress 상태 확인:**
```bash
kubectl get ingress
kubectl describe ingress monolitic-stack-stack-monolitic-ingress
```

**Nginx Ingress Controller가 NodePort로 설치된 경우:**
```bash
# 서버 IP와 NodePort를 사용하여 접근
# HTTP: http://10.137.0.195:30080
# HTTPS: https://10.137.0.195:30443

# 또는 localhost로 접근하려면 Host 헤더 추가
curl -H "Host: localhost" http://10.137.0.195:30080/actuator/health
```

**Ingress 문제 해결:**
```bash
# Ingress Controller 로그 확인
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller

# Ingress 이벤트 확인
kubectl describe ingress monolitic-stack-stack-monolitic-ingress
```
