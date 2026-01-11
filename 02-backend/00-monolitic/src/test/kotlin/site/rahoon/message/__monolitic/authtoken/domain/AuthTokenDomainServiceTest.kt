package site.rahoon.message.__monolitic.authtoken.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import site.rahoon.message.__monolitic.common.domain.DomainException
import java.time.LocalDateTime
import java.util.UUID

/**
 * AuthTokenDomainService 단위 테스트
 * 의존성을 Mock으로 처리하여 도메인 로직만 검증합니다.
 */
class AuthTokenDomainServiceTest {

    private lateinit var accessTokenIssuer: AccessTokenIssuer
    private lateinit var accessTokenVerifier: AccessTokenVerifier
    private lateinit var refreshTokenIssuer: RefreshTokenIssuer
    private lateinit var authTokenRepository: AuthTokenRepository
    private lateinit var authTokenDomainService: AuthTokenDomainService

    @BeforeEach
    fun setUp() {
        accessTokenIssuer = mock()
        accessTokenVerifier = mock()
        refreshTokenIssuer = mock()
        authTokenRepository = mock()
        authTokenDomainService = AuthTokenDomainService(
            accessTokenIssuer,
            accessTokenVerifier,
            refreshTokenIssuer,
            authTokenRepository
        )
    }

    @Test
    fun `토큰 발급 성공 - 새로운 세션 ID 생성`() {
        // given
        val userId = "user123"
        val sessionId = UUID.randomUUID().toString()
        val accessToken = AccessToken(
            token = "access-token",
            expiresAt = LocalDateTime.now().plusHours(1),
            userId = userId,
            sessionId = sessionId
        )
        val refreshToken = RefreshToken(
            token = "refresh-token",
            expiresAt = LocalDateTime.now().plusDays(14),
            userId = userId,
            sessionId = sessionId,
            createdAt = LocalDateTime.now()
        )

        whenever(accessTokenIssuer.issue(any(), any())).thenReturn(accessToken)
        whenever(refreshTokenIssuer.issue(any(), any())).thenReturn(refreshToken)
        whenever(authTokenRepository.saveRefreshToken(any<RefreshToken>())).thenReturn(refreshToken)

        // when
        val result = authTokenDomainService.issueToken(userId)

        // then
        assertNotNull(result)
        assertEquals(accessToken, result.accessToken)
        assertEquals(refreshToken, result.refreshToken)
        assertEquals(userId, result.accessToken.userId)
        assertEquals(userId, result.refreshToken?.userId)
        
        verify(accessTokenIssuer).issue(any(), any())
        verify(refreshTokenIssuer).issue(any(), any())
        verify(authTokenRepository).saveRefreshToken(any<RefreshToken>())
    }

    @Test
    fun `토큰 발급 성공 - 기존 세션 ID 사용`() {
        // given
        val userId = "user123"
        val prevSessionId = "existing-session-id"
        val accessToken = AccessToken(
            token = "access-token",
            expiresAt = LocalDateTime.now().plusHours(1),
            userId = userId,
            sessionId = prevSessionId
        )
        val refreshToken = RefreshToken(
            token = "refresh-token",
            expiresAt = LocalDateTime.now().plusDays(14),
            userId = userId,
            sessionId = prevSessionId,
            createdAt = LocalDateTime.now()
        )

        whenever(accessTokenIssuer.issue(userId, prevSessionId)).thenReturn(accessToken)
        whenever(refreshTokenIssuer.issue(userId, prevSessionId)).thenReturn(refreshToken)
        whenever(authTokenRepository.saveRefreshToken(refreshToken)).thenReturn(refreshToken)

        // when
        val result = authTokenDomainService.issueToken(userId, prevSessionId)

        // then
        assertNotNull(result)
        assertEquals(accessToken, result.accessToken)
        assertEquals(refreshToken, result.refreshToken)
        assertEquals(prevSessionId, result.accessToken.sessionId)
        assertEquals(prevSessionId, result.refreshToken?.sessionId)
        
        verify(accessTokenIssuer).issue(userId, prevSessionId)
        verify(refreshTokenIssuer).issue(userId, prevSessionId)
        verify(authTokenRepository).saveRefreshToken(refreshToken)
    }

    @Test
    fun `AccessToken 검증 성공`() {
        // given
        val tokenString = "valid-access-token"
        val accessToken = AccessToken(
            token = tokenString,
            expiresAt = LocalDateTime.now().plusHours(1),
            userId = "user123",
            sessionId = "session123"
        )

        whenever(accessTokenVerifier.verify(tokenString)).thenReturn(accessToken)

        // when
        val result = authTokenDomainService.verifyAccessToken(tokenString)

        // then
        assertNotNull(result)
        assertEquals(accessToken, result)
        verify(accessTokenVerifier).verify(tokenString)
    }

    @Test
    fun `세션 ID로 토큰 만료 성공`() {
        // given
        val sessionId = "session123"

        // when
        authTokenDomainService.expireBySessionId(sessionId)

        // then
        verify(authTokenRepository).deleteRefreshTokenBySessionId(sessionId)
        verifyNoMoreInteractions(authTokenRepository)
    }

    @Test
    fun `리프레시 토큰으로 새 토큰 발급 성공`() {
        // given
        val refreshTokenString = "valid-refresh-token"
        val userId = "user123"
        val sessionId = "session123"
        val oldRefreshToken = RefreshToken(
            token = refreshTokenString,
            expiresAt = LocalDateTime.now().plusDays(14),
            userId = userId,
            sessionId = sessionId,
            createdAt = LocalDateTime.now().minusDays(1)
        )
        val newAccessToken = AccessToken(
            token = "new-access-token",
            expiresAt = LocalDateTime.now().plusHours(1),
            userId = userId,
            sessionId = sessionId
        )
        val newRefreshToken = RefreshToken(
            token = "new-refresh-token",
            expiresAt = LocalDateTime.now().plusDays(14),
            userId = userId,
            sessionId = sessionId,
            createdAt = LocalDateTime.now()
        )

        whenever(authTokenRepository.findRefreshToken(refreshTokenString)).thenReturn(oldRefreshToken)
        whenever(accessTokenIssuer.issue(userId, sessionId)).thenReturn(newAccessToken)
        whenever(refreshTokenIssuer.issue(userId, sessionId)).thenReturn(newRefreshToken)
        whenever(authTokenRepository.saveRefreshToken(newRefreshToken)).thenReturn(newRefreshToken)

        // when
        val result = authTokenDomainService.refresh(refreshTokenString)

        // then
        assertNotNull(result)
        assertEquals(newAccessToken, result.accessToken)
        assertEquals(newRefreshToken, result.refreshToken)
        assertEquals(userId, result.accessToken.userId)
        assertEquals(sessionId, result.accessToken.sessionId)
        
        verify(authTokenRepository).findRefreshToken(refreshTokenString)
        verify(authTokenRepository).deleteRefreshToken(refreshTokenString)
        verify(authTokenRepository).saveRefreshToken(newRefreshToken)
        verify(accessTokenIssuer).issue(userId, sessionId)
        verify(refreshTokenIssuer).issue(userId, sessionId)
    }

    @Test
    fun `리프레시 토큰이 존재하지 않을 때 예외 발생`() {
        // given
        val invalidRefreshToken = "invalid-refresh-token"
        whenever(authTokenRepository.findRefreshToken(invalidRefreshToken)).thenReturn(null)

        // when & then
        val exception = assertThrows<DomainException> {
            authTokenDomainService.refresh(invalidRefreshToken)
        }
        
        assertEquals(AuthTokenError.INVALID_TOKEN, exception.error)
        assertTrue(exception.details?.containsKey("refreshToken") == true)
        assertEquals(invalidRefreshToken, exception.details?.get("refreshToken"))
        
        verify(authTokenRepository).findRefreshToken(invalidRefreshToken)
        verify(authTokenRepository, never()).deleteRefreshToken(any())
        verify(authTokenRepository, never()).saveRefreshToken(any<RefreshToken>())
        verify(accessTokenIssuer, never()).issue(any(), any())
        verify(refreshTokenIssuer, never()).issue(any(), any())
    }
}

