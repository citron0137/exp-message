package site.rahoon.message.monolithic.channel.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.channel.application.ChannelApplicationService
import site.rahoon.message.monolithic.channel.application.ChannelCriteria
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.auth.CommonAuthRole
import site.rahoon.message.monolithic.common.controller.CommonApiResponse
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException

/**
 * Admin Channel Controller
 * 채널 생성, 수정, 삭제 API (ADMIN 전용)
 */
@RestController
@RequestMapping("/admin/channels")
class AdminChannelController(
    private val channelApplicationService: ChannelApplicationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: AdminChannelRequest.Create,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<AdminChannelResponse.Create> {
        requireAdmin(authInfo)
        val criteria = request.toCriteria()
        val channelInfo = channelApplicationService.create(criteria)
        val response = AdminChannelResponse.Create.from(channelInfo)
        return CommonApiResponse.success(response)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: AdminChannelRequest.Update,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<AdminChannelResponse.Detail> {
        requireAdmin(authInfo)
        val criteria = request.toCriteria(id)
        val channelInfo = channelApplicationService.update(criteria)
        val response = AdminChannelResponse.Detail.from(channelInfo)
        return CommonApiResponse.success(response)
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: String,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<AdminChannelResponse.Detail> {
        requireAdmin(authInfo)
        val criteria = ChannelCriteria.Delete(channelId = id)
        val channelInfo = channelApplicationService.delete(criteria)
        val response = AdminChannelResponse.Detail.from(channelInfo)
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
