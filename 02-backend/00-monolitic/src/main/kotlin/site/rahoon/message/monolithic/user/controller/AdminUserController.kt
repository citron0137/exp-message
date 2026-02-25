package site.rahoon.message.monolithic.user.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.auth.CommonAuthRole
import site.rahoon.message.monolithic.common.controller.CommonApiResponse
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.user.application.UserApplicationService

/**
 * Admin User Controller
 * 사용자 역할 업데이트 API (ADMIN 전용)
 */
@RestController
@RequestMapping("/admin/users")
class AdminUserController(
    private val userApplicationService: UserApplicationService,
) {
    /**
     * 사용자 역할 업데이트
     * PUT /admin/users/{userId}/role
     */
    @PutMapping("/{userId}/role")
    fun updateRole(
        @PathVariable userId: String,
        @Valid @RequestBody request: AdminUserRequest.UpdateRole,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<AdminUserResponse.Detail> {
        requireAdmin(authInfo)
        val userInfo = userApplicationService.updateRole(userId, request.toUserRole())
        val response = AdminUserResponse.Detail.from(userInfo)
        return CommonApiResponse.success(response)
    }

    private fun requireAdmin(authInfo: CommonAuthInfo) {
        if (authInfo.role != CommonAuthRole.ADMIN) {
            throw DomainException(
                error = CommonError.FORBIDDEN,
                details = mapOf("reason" to "Admin role required"),
            )
        }
    }
}
