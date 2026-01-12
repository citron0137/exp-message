package site.rahoon.message.__monolitic.authtoken.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.__monolitic.authtoken.application.AuthTokenApplicationService
import site.rahoon.message.__monolitic.common.controller.ApiResponse

/**
 * 인증 관련 Controller
 * 로그인, 토큰 갱신, 로그아웃 API 제공
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authTokenApplicationService: AuthTokenApplicationService
) {

    /**
     * 로그인
     * POST /auth/login
     */
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AuthRequest.Login
    ): ResponseEntity<ApiResponse<AuthResponse.Login>> {
        val authToken = authTokenApplicationService.login(
            email = request.email,
            password = request.password
        )
        val response = AuthResponse.Login.from(authToken)
        
        return ResponseEntity.status(HttpStatus.OK).body(
            ApiResponse.success(response)
        )
    }
}

