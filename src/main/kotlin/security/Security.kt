package org.burgas.security

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.plugins.csrf.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import org.burgas.database.Authority
import org.burgas.dto.AuthSession
import org.burgas.dto.ExceptionResponse

fun Application.configureSecurity() {

    val config = ApplicationConfig("application.yaml")

    install(DoubleReceive)

    install(StatusPages) {
        exception<Exception> { call, cause ->
            val exceptionResponse = ExceptionResponse(
                status = HttpStatusCode.BadRequest.description,
                code = HttpStatusCode.BadRequest.value,
                message = cause.message
            )
            call.respond(HttpStatusCode.BadRequest, exceptionResponse)
        }
    }

    install(Sessions) {
        cookie<AuthSession>("AUTH_SESSION") {
            cookie.encoding = CookieEncoding.BASE64_ENCODING
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = false
            val encryptionKey = config.property("cookie.auth.encryption").getString()
            val signKey = config.property("cookie.auth.sign").getString()
            transform(
                SessionTransportTransformerEncrypt(encryptionKey.toByteArray(), signKey.toByteArray())
            )
        }
    }

    install(CSRF) {
        originMatchesHost()
        allowOrigin("http://localhost:8000")
    }

    authentication {
        session<AuthSession>("auth-session-all") {
            validate { it }
            challenge {
                val exceptionResponse = ExceptionResponse(
                    status = HttpStatusCode.Unauthorized.description,
                    code = HttpStatusCode.Unauthorized.value,
                    message = "Not authenticated with \"auth-session-all\""
                )
                call.respond(HttpStatusCode.Unauthorized, exceptionResponse)
            }
        }
        session<AuthSession>("auth-session-admin") {
            validate { if (it.authority == Authority.ADMIN) it else null }
            challenge {
                val exceptionResponse = ExceptionResponse(
                    status = HttpStatusCode.Unauthorized.description,
                    code = HttpStatusCode.Unauthorized.value,
                    message = "Not authenticated with \"auth-session-admin\""
                )
                call.respond(HttpStatusCode.Unauthorized, exceptionResponse)
            }
        }
    }
}