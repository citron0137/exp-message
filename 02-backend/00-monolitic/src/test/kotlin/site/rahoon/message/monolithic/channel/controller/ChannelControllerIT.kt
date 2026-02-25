package site.rahoon.message.monolithic.channel.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.common.test.assertError
import site.rahoon.message.monolithic.common.test.assertSuccess

/**
 * Channel Controller 통합 테스트
 * 채널 생성, 조회, 수정, 삭제 API에 대한 전체 스택 테스트
 */
class ChannelControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    override val logger = KotlinLogging.logger {}

    private fun baseUrl(): String = "http://localhost:$port/channels"

    /**
     * 채널을 생성하고 ID를 반환합니다.
     */
    private fun createChannel(
        accessToken: String,
        name: String,
    ): String {
        val request = ChannelRequest.Create(name = name)
        val headers =
            HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        val response =
            restTemplate.exchange(
                baseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        return response
            .assertSuccess<ChannelResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
                data.name shouldBe name
            }.id
    }

    // ===========================================
    // 채널 생성 테스트
    // ===========================================

    @Test
    fun `채널 생성 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val request = ChannelRequest.Create(name = "테스트 채널")
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                baseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
            data.name shouldBe "테스트 채널"
            data.id shouldNotBe null
            data.apiKey shouldNotBe null
            data.apiKey!!.startsWith("ch_") shouldBe true
            data.createdAt shouldNotBe null
            data.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `채널 생성 실패 - 인증 없음`() {
        // given
        val request = ChannelRequest.Create(name = "테스트 채널")
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                baseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `채널 생성 실패 - 이름 누락`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val request = mapOf<String, Any>()
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                baseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST)
    }

    // ===========================================
    // 채널 조회 테스트 (ID)
    // ===========================================

    @Test
    fun `채널 ID로 조회 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val channelId = createChannel(authResult.accessToken, "조회 테스트 채널")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/$channelId",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe channelId
            data.name shouldBe "조회 테스트 채널"
            data.apiKey shouldNotBe null
        }
    }

    @Test
    fun `채널 ID로 조회 실패 - 존재하지 않는 채널`() {
        // given
        val entity = HttpEntity<Nothing?>(null, HttpHeaders())

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/non-existent-id",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_001")
    }

    // ===========================================
    // 채널 조회 테스트 (apiKey)
    // ===========================================

    @Test
    fun `채널 apiKey로 조회 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val channelId = createChannel(authResult.accessToken, "apiKey 조회 테스트 채널")
        val getByIdEntity = HttpEntity<Nothing?>(null, authResult.headers)
        val getByIdResponse =
            restTemplate.exchange(
                "${baseUrl()}/$channelId",
                HttpMethod.GET,
                getByIdEntity,
                String::class.java,
            )
        val channelDetail = getByIdResponse.assertSuccess<ChannelResponse.Detail>(objectMapper, HttpStatus.OK) { }
        val apiKey = channelDetail.apiKey!!

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}?apiKey=$apiKey",
                HttpMethod.GET,
                getByIdEntity,
                String::class.java,
            )

        // then
        response.assertSuccess<List<ChannelResponse.Detail>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.shouldHaveSize(1)
            dataList[0].id shouldBe channelId
            dataList[0].name shouldBe "apiKey 조회 테스트 채널"
            dataList[0].apiKey shouldBe apiKey
        }
    }

    @Test
    fun `채널 apiKey로 조회 - 존재하지 않는 apiKey`() {
        // given
        val entity = HttpEntity<Nothing?>(null, HttpHeaders())

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}?apiKey=ch_invalid_key_12345",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_001")
    }

    // ===========================================
    // 채널 수정 테스트
    // ===========================================

    @Test
    fun `채널 수정 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val channelId = createChannel(authResult.accessToken, "원래 이름")

        val updateRequest = ChannelRequest.Update(name = "수정된 이름")
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/$channelId",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.name shouldBe "수정된 이름"
            data.id shouldBe channelId
        }
    }

    @Test
    fun `채널 수정 실패 - 인증 없음`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val channelId = createChannel(authResult.accessToken, "원래 이름")

        val updateRequest = ChannelRequest.Update(name = "수정 시도")
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/$channelId",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `채널 수정 실패 - 존재하지 않는 채널`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val updateRequest = ChannelRequest.Update(name = "수정 시도")
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/non-existent-id",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_001")
    }

    // ===========================================
    // 채널 삭제 테스트
    // ===========================================

    @Test
    fun `채널 삭제 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val channelId = createChannel(authResult.accessToken, "삭제될 채널")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/$channelId",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe channelId
        }

        // 삭제 후 조회 시 404 확인
        val getResponse =
            restTemplate.exchange(
                "${baseUrl()}/$channelId",
                HttpMethod.GET,
                entity,
                String::class.java,
            )
        getResponse.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `채널 삭제 실패 - 인증 없음`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val channelId = createChannel(authResult.accessToken, "삭제될 채널")
        val entity = HttpEntity<Nothing?>(null, HttpHeaders())

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/$channelId",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED

        // 삭제되지 않았는지 확인
        val getEntity = HttpEntity<Nothing?>(null, authResult.headers)
        val getResponse =
            restTemplate.exchange(
                "${baseUrl()}/$channelId",
                HttpMethod.GET,
                getEntity,
                String::class.java,
            )
        getResponse.statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `채널 삭제 실패 - 존재하지 않는 채널`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/non-existent-id",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_001")
    }
}
