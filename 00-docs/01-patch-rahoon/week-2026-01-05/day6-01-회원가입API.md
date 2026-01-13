# 패치노트 - 2026-01-10

## 회원가입 API 구현

### 구현 내용

#### 1. UserController 및 DTO

**생성된 파일:**
- `UserController.kt`: `POST /users` - 회원가입
- `UserRequest.kt`: `UserRequest.SignUp` (email, password, nickname)
- `UserResponse.kt`: `UserResponse.SignUp` (id, email, nickname, createdAt)

**변환 메서드:**
- `UserRequest.SignUp.toCriteria()`: UserCriteria.Register로 변환
- `UserResponse.SignUp.from()`: UserInfo.Detail로부터 생성

#### 2. 예외 처리 개선

- `HttpMessageNotReadableException` 핸들러 추가: JSON 파싱 오류 처리 (400 Bad Request)

#### 3. LocalDateTime 직렬화

- `JacksonConfig`에 `LocalDateTimeSerializer` 추가: ISO-8601 형식 직렬화

#### 4. E2E 테스트

- 회원가입 성공/실패 케이스 테스트 추가

### API 엔드포인트

- `POST /api/users` - 회원가입

### 설계 결정

- RESTful 설계: `POST /api/users` (사용자 리소스 생성)
- DTO 구조: `UserRequest.SignUp`, `UserResponse.SignUp` (Controller 패키지에 위치)
