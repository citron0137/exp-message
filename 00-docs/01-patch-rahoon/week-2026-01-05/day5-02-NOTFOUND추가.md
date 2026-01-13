# 패치노트 - 2026-01-09

## NoResourceFoundException 404 처리 추가

### 문제 상황

**발생한 이슈:**

- 유효하지 않은 리소스로 요청 시 `NoResourceFoundException` 발생
- 현재는 500 Internal Server Error로 처리되어 부적절한 응답 코드 사용
- 예: `GET /api/123` → 500 Internal Server Error

**에러 로그 예시:**

```
2026-01-09T07:58:11.125Z ERROR 1 --- [00-monolitic] [nio-8080-exec-4] s.r.m._.c.c.GlobalExceptionHandler       : Internal Server Error
├─ Request: GET /api/123
├─ Exception: NoResourceFoundException
└─ Message: No static resource 123.
```

**기존 응답:**

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INTERNAL_SERVER_ERROR",
    "message": "서버 오류가 발생했습니다",
    "details": null,
    "occurredAt": "2026-01-09T07:58:11.130063847Z",
    "path": "/api/123"
  }
}
```

### 해결 방법

**구현 내용:**

- `GlobalExceptionHandler`에 `NoResourceFoundException` 전용 핸들러 추가
- 404 NOT_FOUND HTTP 상태 코드로 응답하도록 변경
- 리소스 경로 정보를 details에 포함하여 디버깅 용이성 향상

**코드 변경 (`GlobalExceptionHandler.kt`):**

```kotlin
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * 리소스를 찾을 수 없을 때 처리 (404)
 */
@ExceptionHandler(NoResourceFoundException::class)
fun handleNoResourceFoundException(
    e: NoResourceFoundException,
    request: HttpServletRequest
): ResponseEntity<ApiResponse<Nothing>> {
    val response = ApiResponse.error<Nothing>(
        code = "NOT_FOUND",
        message = "요청한 리소스를 찾을 수 없습니다",
        details = mapOf("resourcePath" to e.resourcePath),
        occurredAt = ZonedDateTime.now(),
        path = request.requestURI
    )

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
}
```

### 변경 후 동작

**개선된 응답:**

- `GET /api/123` → 404 NOT_FOUND 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "NOT_FOUND",
    "message": "요청한 리소스를 찾을 수 없습니다",
    "details": {
      "resourcePath": "/123"
    },
    "occurredAt": "2026-01-09T07:58:11.130063847Z",
    "path": "/api/123"
  }
}
```

**개선 사항:**

1. ✅ 적절한 HTTP 상태 코드 사용 (500 → 404)
2. ✅ 명확한 에러 메시지 제공
3. ✅ 리소스 경로 정보 포함으로 디버깅 용이
4. ✅ RESTful API 표준에 부합하는 응답

### 참고사항

- Spring 6.0+ 에서 도입된 `NoResourceFoundException` 사용
- 정적 리소스를 찾을 수 없을 때 발생하는 예외를 적절히 처리
- 다른 예외 핸들러와 동일한 `ApiResponse` 형식으로 일관성 유지

