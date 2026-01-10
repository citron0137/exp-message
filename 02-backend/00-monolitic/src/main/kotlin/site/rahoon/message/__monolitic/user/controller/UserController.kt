package site.rahoon.message.__monolitic.user.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.__monolitic.common.controller.ApiResponse
import site.rahoon.message.__monolitic.user.application.UserApplicationService
import site.rahoon.message.__monolitic.user.application.UserCriteria

/**
 * 사용자 관련 Controller
 * 회원가입 및 사용자 관리 API 제공
 */
@RestController
@RequestMapping("/users")
class UserController(
    private val userApplicationService: UserApplicationService
) {

    /**
     * 회원가입
     * POST /api/users
     */
    @PostMapping
    fun signUp(
        @Valid @RequestBody request: UserRequest.SignUp
    ): ResponseEntity<ApiResponse<UserResponse.SignUp>> {
        val criteria = request.toCriteria()
        val userInfo = userApplicationService.register(criteria)
        val response = UserResponse.SignUp.from(userInfo)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(response)
        )
    }
}

