/*
 * Copyright (c) 2024.
 * @Author Phel Viwath
 */

package sru.edu.sru_lib_management.auth.domain.jwt

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import sru.edu.sru_lib_management.common.InvalidBearerToken

@Component
class JwtAuthenticationManager(
    private val users: ReactiveUserDetailsService,
    private val jwtToken: JwtToken
) : ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication?): Mono<Authentication> {
        return Mono.justOrEmpty(authentication)
            .filter {
                it is BearerToken
            }
            .cast(BearerToken::class.java)
            .flatMap { token ->
                mono { validate(token) }
            }
            .onErrorMap { error ->
                InvalidBearerToken(error.message)
            }
    }

    private suspend fun validate(token: BearerToken): Authentication{
        val email = jwtToken.extractEmail(token)
        val user = users.findByUsername(email).awaitSingleOrNull()

        if (jwtToken.isValidToken(token, user)){
            return UsernamePasswordAuthenticationToken(
                user!!.username,
                user.password,
                user.authorities
            )
        }
        throw IllegalArgumentException("Token is not valid.")
    }
}