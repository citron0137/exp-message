# 패치노트 - 2026-01-13

## 목표했던 내용 1
- 로그인 API에 대한 무차별 대입 공격(Brute Force Attack) 방지
- 로그인 실패 횟수 추적 및 일정 횟수 이상 실패 시 일시적 로그인 제한

## 변경사항 1
- `loginfailure` 독립 도메인 패키지 생성
  - `LoginFailure`: 도메인 모델 (비즈니스 로직 캡슐화)
  - `LoginFailureRepository`: 저장소 인터페이스
  - `LoginFailureTracker`: 실패 횟수 추적 도메인 서비스
  - `LoginFailureError`: 도메인 에러 (`ACCOUNT_LOCKED`)
  - `LoginFailureRepositoryImpl`: Redis 기반 구현
- Redis 기반 실패 횟수 추적 (IP/이메일 이중 추적, 최대 5회, 잠금 15분)
- `IpAddressUtils`: IP 주소 추출 유틸리티 추가
- `AuthController.login()`: IP 주소 추출하여 전달
- `AuthTokenApplicationService.login()`: 실패 횟수 확인/증가/초기화 로직 추가

## UX 동작
- 정상 로그인: 실패 횟수 자동 초기화
- 로그인 실패 (1~4회): 일반 에러 응답, 실패 횟수 증가 (15분동안 보관)
- 로그인 실패 (5회): 계정 잠금 에러 (`ACCOUNT_LOCKED`), 15분 동안 로그인 불가
- 잠금 해제: 15분 후 자동 해제 또는 로그인 성공 시 즉시 해제

## 피드백

### 잘한 점

1. **독립 도메인 패키지 분리** - `loginfailure` 패키지로 도메인 독립성 확보 및 authtoken과의 결합도 감소
2. **도메인 모델로 비즈니스 로직 캡슐화** - `LoginFailure` 도메인 모델에 잠금 로직 포함, 도메인 에러 별도 정의
3. **이중 추적 전략** - IP 주소와 이메일 기반 이중 추적으로 보안 강화
4. **TTL 기반 자동 만료** - Redis TTL로 메모리 관리 자동화 및 자동 해제
5. **catch 블록 최소화** - `getUser()` 호출만 catch로 감싸 정확한 실패 추적

### 아쉬운 점

1. **E2E 테스트 부재** - 로그인 실패 횟수 추적 및 잠금 기능에 대한 E2E 테스트 없음
2. **점진적 잠금 시간 미구현** - 고정된 잠금 시간(15분)만 적용, 반복 실패 시 잠금 시간 증가 기능 부재
3. **관리자 기능 부재** - 계정 잠금 해제 API 및 잠금 이력 조회 기능 없음
4. **사용자 알림 부재** - 계정 잠금 시 이메일 알림 및 잠금 해제 안내 메시지 없음
