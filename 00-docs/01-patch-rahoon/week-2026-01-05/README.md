# 주간 패치노트 (2026-01-05 ~ 2026-01-11)

## 1. 이번 주 진행한 내용
- **프로젝트 기반/문서 체계 구축**
  - 프로젝트 소개 `README.md` 작성 및 목표(“아주 튼튼한 채팅 서버”) 명시
  - 패치노트 디렉토리/가이드 구축 (`00-docs/patch/README.md`) 및 템플릿 정리
  - 프로젝트 전체 디렉토리 구조 초안 정리(Infra/Backend/Frontend/Test/Scripts/Docs)

- **기술 스택/아키텍처 의사결정 정리**
  - 프론트엔드: React + TypeScript 선택(백엔드 검증용 최소 UI)
  - 백엔드: Kotlin + Spring Boot 중심으로 통일(필요 시 Go/NestJS 후보)
  - 데이터: MySQL 선택 + Redis/Elasticsearch/Kafka/모니터링(Grafana 계열)/부하테스트(k6) 방향성 확정
  - MSA 목표 구조(게이트웨이/User/Chat/Message/Search/Notification/WebSocket) 및 데이터 흐름 정리
  - Database per Service 패턴 및 샤딩/파티셔닝(애플리케이션 레벨 우선 검토) 원칙 정리

- **인프라/배포 기반 구성**
  - 인프라 디렉토리 설계 원칙 정리 및 구조 문서화
  - 로컬 개발환경: Docker Compose 기반 MySQL 구성 방향 + binlog 기반 복구 전략 정리
  - k3s + Helm + ingress-nginx 설치/검증 및 로컬 레지스트리(insecure) 설정 절차 문서화
  - 모놀리식 앱 Helm 차트 구성 + Umbrella(스택) 구조 통합(MySQL + App + Ingress)
  - 배포 스크립트/차트 통합 구성(`05-scripts/02-deploy-monolitic/`) 및 환경변수(.env) 기반 배포 흐름 정리
  - 실서버 인증서 만료 이슈 해결(OPNsense ACME Client로 신규 발급/적용/자동갱신 설정)
  - Ingress 경로 처리 보강(`/api` rewrite + `X-Forwarded-Prefix`로 redirect 경로 문제 해결)

- **백엔드(모놀리식) 구현 진척**
  - 공통 예외/에러 모델 정리(DomainError/DomainException/ApplicationException) 및 트랜잭션 유틸(`Tx`) 구성
  - User 도메인 구현(엔티티/커맨드/도메인 서비스/검증/비밀번호 해싱)
  - JPA 기반 `UserRepository` 구현(Entity/JpaRepository/Impl) + 통합 테스트 작성(H2)
  - 공통 API 응답 템플릿(`ApiResponse`) + 전역 예외 처리 + Health API + E2E 테스트 구성
  - 실행 환경: MySQL datasource 설정/환경변수 주입, JDBC 드라이버/Actuator 추가, `bootRun`의 `.env` 로드 지원
  - Dockerfile(멀티 스테이지), `.dockerignore`, docker-compose(MySQL + App) 작성
  - Swagger UI(OpenAPI) 추가로 운영 환경에서 API 문서/테스트 가능하도록 구성
  - E2E/점검용 `TestController` 추가
  - `NoResourceFoundException` 404 처리 추가(유효하지 않은 리소스 요청 시 500 → 404)

## 2. AI 피드백
### 잘한점
- **“실행/배포/운영”까지 이어지는 기반을 실제로 닫았음**
  - docker-compose(MySQL+App) + `.env` 기반 설정 주입 + Dockerfile(멀티 스테이지)로 로컬 실행 루프를 만들었고, k3s/Helm/ingress-nginx/레지스트리까지 연결해 “올릴 수 있는 상태”를 만든 점이 큼
- **Ingress 환경에서 자주 터지는 문제를 초기에 잡음**
  - `/api` rewrite와 함께 `X-Forwarded-Prefix`를 써서 redirect 경로 문제를 해결한 것은, Swagger UI/리다이렉트가 섞이는 실제 운영에서 효과가 바로 나는 개선임
- **API 품질(일관성/디버깅 용이성)을 초반에 표준화함**
  - `ApiResponse` + 전역 예외 처리 + Health/E2E 테스트로 응답 포맷/시간 직렬화/상태코드 매핑을 일관되게 잡아둔 덕분에, 이후 기능이 늘어도 “응답 품질”이 무너지기 어렵다
  - 특히 `NoResourceFoundException`을 404로 분리 처리해서 “잘못된 경로 요청 = 500” 같은 운영 노이즈를 줄였음
- **배포 설정의 변경 속도를 올리는 구조 개선을 함**
  - Umbrella 구조/배포 스크립트를 `05-scripts/02-deploy-monolitic/`로 통합해 “자주 바뀌는 배포 설정”을 한 군데에서 빠르게 반영할 수 있게 만든 건 이후 반복 작업 비용을 줄여줌
- **운영 이슈 대응 경험을 남김**
  - 인증서 만료를 OPNsense ACME Client로 재발급/적용/자동 갱신까지 처리한 건 실서비스 관점에서 설득력 있는 성과임
- **생산성 관점에서 ‘나중에 발목 잡힐 것’을 먼저 제거했음**
  - 초기에 Swagger UI/헬스체크/E2E/표준 응답/Ingress 경로 이슈까지 함께 잡아둔 덕분에, 이후 기능 개발 시 “배포 후에 원인 찾느라 쓰는 시간(디버깅/재배포)”을 크게 줄일 수 있는 방향의 우선순위였음

### 아쉬운점
- **‘기반’은 강해졌는데, 시연 가능한 “완주 기능”이 아직 얇음**
  - User 도메인/Repository/테스트/응답 표준은 잘 갖춰졌지만, 실제로 보여줄 “회원가입/로그인(JWT)/내정보” 같은 API 사이클이 아직 부족해 보일 수 있음
- **배포/검증의 “증거”를 더 남기면 설득력이 급상승함**
  - Swagger UI 접근, `/api/health` 호출, Helm 설치/업그레이드 로그, Ingress rewrite/forwarded-prefix 동작 확인 같은 결과를 캡처해 두면 “말”이 아니라 “증거”로 설명 가능
- **계획/절차 문서와 완료 산출물의 경계가 흐려질 수 있음**
  - 예: DB 구축 문서는 절차 성격이 강해서, 실제 적용/검증(명령/출력/이슈/해결)을 별도 섹션으로 분리하면 “완료”가 더 명확해짐
- **문서/파일 네이밍 정리는 여전히 필요**
  - 중복 문서와 오타성 파일명(예: `기슬`)은 탐색성을 떨어뜨리니, 주차 마감 시점에 정리하면 좋음
- **생산성 관점에서 ‘더 먼저 했으면 좋은 것’은 최소 기능 1사이클 완주 + 배포 증거 확보**
  - 기반을 어느 정도 확보한 시점(주 중반)부터는 **회원가입 → 로그인(JWT) → 내정보 조회** 같은 “끝까지 동작하는 유스케이스 1개”를 먼저 완주하고,
  - 그 유스케이스를 **Ingress 경로로 실제 호출(health + Swagger 포함)** 해서 성공 로그/스크린샷/명령 출력까지 남겼으면, 같은 시간 대비 산출물 설득력이 더 컸을 가능성이 큼

## 3. 앞으로 진행할 내용 (우선순위 높은순서)
- **최소 유스케이스 1사이클 완주 + “증거” 남기기 (가장 높은 생산성)**
  - **회원가입 → 로그인(JWT) → 내 정보 조회**까지 API 1사이클을 먼저 완성(컨트롤러 포함)
  - 검증 체크리스트(문서/로그/스크린샷 형태로 남기기)
    - docker-compose로 App+MySQL 기동 → curl로 1사이클 호출 성공
    - k3s+Helm 배포 → Ingress 경로(`/api`)로 1사이클 호출 성공
    - `/api/health` 및 Swagger UI 접근 성공, `X-Forwarded-Prefix` 동작(리다이렉트 경로) 확인
    - 잘못된 경로 요청 시 404 응답(`NoResourceFoundException`) 확인

- **배포/검증 루프를 “기능과 함께” 고정**
  - 로컬(Compose) ↔ 클러스터(Helm/Ingress)에서 동일 시나리오를 반복 가능하게 만들고,
  - 실패 케이스는 “원인/해결/재발 방지”까지 짧게 기록(다음날 재현/회고 가능하게)

- **인프라 변경사항 정리 및 반영(리뷰 가능한 단위로)**
  - 금요일 인프라 개선(Helm/Ingress/배포 스크립트/Swagger 경로 등)을 변경 요약과 함께 정리하고,
  - 리뷰/추적이 쉬운 단위로 묶어서 반영(커밋/PR 단위 정돈)

- **프론트엔드(시연용 최소 UI) — API 1사이클이 붙는 즉시**
  - 목적은 “제품”이 아니라 **시연**: 로그인 → 내 정보(→ 채팅방 목록은 이후) 정도의 최소 화면
  - 범위를 키우면 생산성이 떨어지므로 “2~3 화면”으로 제한

- **보안/운영 기본기 보강**
  - Security: `health`, `swagger`만 공개하고 나머지는 인증 필요로 전환
  - CORS 정책, 요청 로깅(구조화 로그), Actuator 노출 정책(최소)

- **DB 스키마/마이그레이션 도입**
  - Flyway(또는 Liquibase)로 `users`부터 초기 마이그레이션 작성
  - 운영 환경에서 `ddl-auto` 의존을 줄이고, 재현 가능한 DB 상태를 만들기

- **채팅 도메인 최소 골격 API 추가**
  - 채팅방: 생성/조회/목록, 멤버: 참가/나가기
  - 메시지: 전송/조회(페이징)까지 “최소 기능” 구현

- **테스트/품질(재현성 강화)**
  - API/도메인 통합 테스트를 “핵심 유스케이스” 중심으로 추가
  - 가능하면 Testcontainers(MySQL)로 로컬/CI 재현성 확보

- **성능/관찰성은 ‘측정 가능한 상태’가 된 뒤에**
  - k6 시나리오(로그인/메시지 전송/조회) + 최소 메트릭/로그 대시보드로 연결

- **문서/파일 네이밍 정리**
  - 중복 문서/오타성 파일명을 정리해 탐색성을 올리고, 주간 요약의 신뢰도를 높이기

