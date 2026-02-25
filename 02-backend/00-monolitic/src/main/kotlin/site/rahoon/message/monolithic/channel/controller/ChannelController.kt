package site.rahoon.message.monolithic.channel.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.channel.application.ChannelApplicationService
import site.rahoon.message.monolithic.channel.application.ChannelCriteria
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.controller.CommonApiResponse

/**
 * Channel Controller
 * 채널(서비스 단위) 생성, 조회, 수정, 삭제 API
 */
@RestController
@RequestMapping("/channels")
class ChannelController(
    private val channelApplicationService: ChannelApplicationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Suppress("UnusedParameter")
    fun create(
        @Valid @RequestBody request: ChannelRequest.Create,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<ChannelResponse.Create> {
        val criteria = request.toCriteria()
        val channelInfo = channelApplicationService.create(criteria)
        val response = ChannelResponse.Create.from(channelInfo)
        return CommonApiResponse.success(response)
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: String,
    ): CommonApiResponse<ChannelResponse.Detail> {
        val channelInfo = channelApplicationService.getById(id)
        val response = ChannelResponse.Detail.from(channelInfo)
        return CommonApiResponse.success(response)
    }

    @GetMapping(params = ["apiKey"])
    fun getByApiKey(
        @RequestParam apiKey: String,
    ): CommonApiResponse<List<ChannelResponse.Detail>> {
        val channelInfo = channelApplicationService.getByApiKey(apiKey)
        val response = listOf(ChannelResponse.Detail.from(channelInfo))
        return CommonApiResponse.success(response)
    }

    @PutMapping("/{id}")
    @Suppress("UnusedParameter")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: ChannelRequest.Update,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<ChannelResponse.Detail> {
        val criteria = request.toCriteria(id)
        val channelInfo = channelApplicationService.update(criteria)
        val response = ChannelResponse.Detail.from(channelInfo)
        return CommonApiResponse.success(response)
    }

    @DeleteMapping("/{id}")
    @Suppress("UnusedParameter")
    fun delete(
        @PathVariable id: String,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<ChannelResponse.Detail> {
        val criteria = ChannelCriteria.Delete(channelId = id)
        val channelInfo = channelApplicationService.delete(criteria)
        val response = ChannelResponse.Detail.from(channelInfo)
        return CommonApiResponse.success(response)
    }
}
