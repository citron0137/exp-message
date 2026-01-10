# 패치노트 - 2026-01-10

## 인증 API 구현 계획

### 도메인 네이밍 결정

도메인 객체명 `AuthToken`과 통일하기 위해 **`authtoken`** 패키지명을 사용하기로 결정.

**이유:**
- 도메인 객체명과 패키지명 통일로 일관성 유지
- user 도메인과 동일한 네이밍 패턴 (user 패키지, User 객체)
- 간결하고 명확한 표현

### 설계 결정

- 도메인 네이밍: `authtoken` (패키지명), `AuthToken` (도메인 객체명)
- API 경로: `POST /authtoken/*` (패키지명과 통일)
- DTO 구조: `AuthTokenRequest.*`, `AuthTokenResponse.*` (Controller 패키지에 위치)

