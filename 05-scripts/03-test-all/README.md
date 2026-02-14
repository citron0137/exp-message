# 03-test-all

백엔드(02-backend/00-monolitic) 전체 검사 실행: clean, detekt, ktlintCheck, test.

## 사용법

저장소 루트에서 실행:

```bash
# 전체 (detekt + ktlintCheck + test, 통합 테스트 포함)
05-scripts/03-test-all/run.sh

# 통합 테스트 제외 (단위 테스트만)
05-scripts/03-test-all/run.sh --except-integration
```

## CI / pre-push

- **CI** (`.github/workflows/ci.yml`): `run.sh --except-integration` 사용 (Docker 없는 환경).
- **pre-push** (`.husky/pre-push`): `run.sh` 사용 (전체 테스트, 로컬에서 Docker 필요).
