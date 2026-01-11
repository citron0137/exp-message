package site.rahoon.message.__monolitic.authtoken.application

import org.springframework.stereotype.Service
import site.rahoon.message.__monolitic.authtoken.domain.AccessToken
import site.rahoon.message.__monolitic.authtoken.domain.AuthToken
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenDomainService

/**
 * AuthToken Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class AuthTokenApplicationService{

    // Login
    fun login(){
        TODO()
    }

    // Check
    fun checkAccessToken(){
        TODO()
    }

    // Refresh
    fun refresh(){
        TODO()
    }

    // Logout
    fun logout(){
        TODO()
    }

}

