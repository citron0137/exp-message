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

## 목표

- accessToken을 **JWT(서명/만료/클레임)** 으로 발급
- logout은 **전체 로그아웃이 아니라 “해당 세션만” 로그아웃** 되도록 설계(sid 기반)
- 레이어 책임 정리: **Application → DomainService → Repository**

## 진행

- **JWT 기반 accessToken 발급** 적용
  - HS256 서명, `iss/sub/iat/exp` + 커스텀 클레임(`typ=access`, `uid`)
  - 토큰 생성 로직을 컴포넌트로 분리: `JwtAccessTokenIssuer`
- **세션 단위 로그아웃을 위한 sid 도입**
  - accessToken(JWT)에 `sid` 포함, `jti`도 함께 포함(토큰 인스턴스 추적용)
  - refresh token 저장에 `sessionId`를 함께 저장하도록 스키마/리포지토리 확장
  - logout은 accessToken에서 `(userId, sid)` 추출 후 **sid 기준 refresh token만 삭제**
  - JWT 파싱/검증 책임 분리: `JwtAccessTokenClaimsExtractor`
- **도메인/애플리케이션 레이어 정리**
  - repository 호출/비즈니스 로직은 `AuthTokenDomainService`로 이동
  - `AuthTokenApplicationService`는 유저 검증 후 도메인 호출만 수행
- **테스트 추가/보강**
  - `JwtAccessTokenIssuer`, `JwtAccessTokenClaimsExtractor`, `AuthTokenDomainService` 단위 테스트 작성

## TODO

- accessToken 즉시 무효화 요구 시, `jti` 또는 `sid` 기준 **블랙리스트(예: Redis, exp까지 TTL)** 적용 여부/범위 결정 (로그아웃 즉시반영 vs 만료까지 허용)
- refresh token도 **회전/재사용 탐지** 전략 확정(토큰 패밀리, reuse 감지 시 세션 폐기 등)
- 세션 관리 기능 필요 여부 결정
  - 예: 세션 목록 조회/특정 sid 강제 로그아웃(디바이스/UA/IP 메타데이터 저장)
- Controller/DTO/E2E 흐름 정리
  - `POST /authtoken/login|refresh|logout` 요청/응답 스펙 확정 및 문서화

