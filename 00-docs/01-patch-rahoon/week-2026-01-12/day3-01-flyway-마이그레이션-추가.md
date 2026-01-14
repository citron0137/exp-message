# Flyway 마이그레이션 추가 (2026-01-14)

## 📋 주요 작업

1. Flyway 도입
2. 테스트코드 개선 - Testcontainers, MockK 도입, Suffix 컨벤션, 테스트 분리
3. Flyway 인프라 구축
4. Deploy 스크립트 작성 및 개선

---

## 🗄️ 1. Flyway 도입

**DB 마이그레이션 도구 Flyway를 도입하여 스키마 버전 관리를 시작했습니다.**

- `01-db-migrations` 모듈 신규 생성
- 마이그레이션 파일 네이밍: `V{날짜}_{순번}__{설명}.sql`
- Spring Boot + Flyway 자동 마이그레이션 실행

```sql
-- 예시: V20260114_01__create_flyway_test_table.sql
CREATE TABLE flyway_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
```

---

## 🧪 2. 테스트코드 개선

### Testcontainers 도입
- **MySQL Container**: 실제 DB 환경에서 통합 테스트
- **Redis Container**: 캐시/세션 테스트
- **Singleton Container Pattern**: 테스트 간 컨테이너 재사용으로 속도 향상
- 마이그레이션 컨테이너 자동 실행 (통합 테스트 시)

### MockK 도입
- Kotlin 친화적 Mocking 프레임워크
- `mockk()`, `every {}`, `verify {}` 활용

### 테스트 Suffix 컨벤션 도입

| Suffix | 설명 | 실행 명령 |
|--------|------|-----------|
| `*UT.kt` | Unit Test (단위 테스트) | `./gradlew unitTest` |
| `*IT.kt` | Integration Test (통합 테스트) | `./gradlew integrationTest` |

### 테스트 인프라 클래스
- `@IntegrationTest`: 통합 테스트 마커 어노테이션
- `IntegrationTestBase`: Testcontainers + 마이그레이션 자동 설정

---

## 🐳 3. Flyway 인프라 구축

### Docker 이미지
- `01-db-migrations/Dockerfile`: 멀티스테이지 빌드
- SQL 파일 변경만 있을 시 캐시 활용 최적화
- Helm chart `batch-db-migration` 추가

### Kubernetes Job
- 앱 배포 전 마이그레이션 Job 실행
- DB 스키마 자동 동기화

---

## 🚀 4. Deploy 스크립트 작성 및 개선

### `docker-build-n-push.ps1`
- App + Migration 이미지 **병렬 빌드**
- 로그 파일 분리 (`.log/docker-build-n-push-*.log`)
- 실시간 진행 상황 모니터링

### `helm-deploy.ps1`

| 명령 | 설명 |
|------|------|
| `.\helm-deploy.ps1 c` | Install (신규 배포) |
| `.\helm-deploy.ps1 u` | Upgrade (업그레이드) |
| `.\helm-deploy.ps1 d` | Uninstall (삭제) |
| `.\helm-deploy.ps1 la` | App 로그 보기 |
| `.\helm-deploy.ps1 lm` | Migration 로그 보기 |
| `.\helm-deploy.ps1 mm` | MySQL 셸 접속 |
| `.\helm-deploy.ps1 kubectl [args]` | kubeconfig 자동 적용 kubectl |

---

## 📁 변경된 파일 구조

```
02-backend/
├── 00-monolitic/
│   └── src/test/kotlin/
│       └── common/test/
│           ├── TestAnnotations.kt     # @IntegrationTest
│           ├── IntegrationTestBase.kt # Testcontainers 설정
│           └── TestUtils.kt
│       └── **/*IT.kt                  # 통합 테스트
│       └── **/*UT.kt                  # 단위 테스트
└── 01-db-migrations/                  # 신규 모듈
    ├── Dockerfile
    └── src/main/resources/db/migration/
        └── V*.sql

05-scripts/02-deploy-monolitic/
├── docker-build-n-push.ps1            # 병렬 빌드
├── helm-deploy.ps1                    # 통합 배포 CLI
└── charts/
    └── batch-db-migration-0.1.0.tgz   # 마이그레이션 차트
```

---

## 💬 피드백

### ✅ 잘한 점

1. **체계적인 버전 관리 도입**
   - Flyway 도입으로 DB 스키마 변경 이력을 코드로 관리
   - 네이밍 컨벤션(`V{날짜}_{순번}__{설명}.sql`)으로 변경 추적 용이

2. **테스트 인프라 현대화**
   - Testcontainers + Singleton Pattern으로 실제 DB 환경 테스트 가능
   - UT/IT 분리로 테스트 목적에 맞는 실행 가능

3. **배포 자동화 및 효율화**
   - 병렬 빌드로 빌드 시간 단축
   - 통합 CLI(`helm-deploy.ps1`)로 운영 명령 단순화
   - 마이그레이션 Job을 앱 배포 전 자동 실행

4. **도커 이미지 최적화**
   - 멀티스테이지 빌드로 이미지 크기 최소화
   - SQL 파일 캐시 레이어 분리로 빌드 속도 향상

5. **Baseline 마이그레이션 수립**
   - `V20260113_00__init.sql`로 기존 스키마 Baseline 작성 완료
   - 신규 환경에서도 동일한 스키마로 시작 가능

6. **패치 마이그레이션 전략 수립**
   - 마이그레이션 실패 또는 버그 발생 시 Undo가 아닌 추가 패치 마이그레이션 작성 방식 채택
   - Forward-only 마이그레이션으로 히스토리 추적 용이

### ⚠️ 아쉬운 점

1. **모니터링/알림 체계 미구축**
   - 마이그레이션 실패 시 Slack/이메일 알림 없음
   - 마이그레이션 히스토리 대시보드 부재

2. **대용량 데이터 처리 고려 부족**
   - 대용량 테이블 스키마 변경 시 락 이슈 미고려
   - 데이터 마이그레이션(DML) 전략 미수립

---

## 🔮 더 해볼 내용

| 주제 | 설명 |
|------|------|
| Spring Batch 연동 | 대용량 데이터 마이그레이션 (청크 단위 처리, 재시작 가능) |
| Flyway 콜백 | 마이그레이션 전/후 처리, 실패 시 Slack 알림 |
| Online DDL | pt-online-schema-change로 무중단 스키마 변경 |
| Dry-Run | `flywayInfo`, `flywayValidate`로 미리보기 |
| Expand-Contract 패턴 | Blue-Green 배포 시 스키마 호환성 유지 |
| 히스토리 대시보드 | Grafana로 `flyway_schema_history` 시각화 |
| 멀티 데이터소스 | 읽기/분석 DB 별도 마이그레이션 관리 |
