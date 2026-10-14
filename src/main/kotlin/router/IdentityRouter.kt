package org.burgas.router

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.burgas.dao.IdentityEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.AuthSession
import org.burgas.dto.IdentityRequest
import org.burgas.service.IdentityService
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureIdentityRouter() {

    val identityService by inject<IdentityService>()

    val identityAuthenticationInterceptor = createRouteScopedPlugin("IdentityAuthenticationInterceptor") {
        on(AuthenticationChecked) { call ->
            when (call.request.path()) {

                "/api/v1/identities/change-status" -> {
                    val authSession = call.sessions.get(AuthSession::class)
                    requireNotNull(authSession) { "AuthSession is null in identity interceptor by change-status" }

                    val identityRequest = call.receive(IdentityRequest::class)
                    val identity = suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        IdentityEntity[identityRequest.id!!]
                    }
                    require(authSession.id != identity.id.value) {
                        "You can't change your own status. Not authorized in identity interceptor by change-status"
                    }
                }

                "/api/v1/identities/by-id", "/api/v1/identities/delete",
                "/api/v1/identities/upload-image", "/api/v1/identities/remove-image" -> {
                    val authSession = call.sessions.get(AuthSession::class)
                    requireNotNull(authSession) { "AuthSession is null in identity interceptor by identityId" }

                    val identityId = Uuid.parse(call.parameters["identityId"]!!)
                    val identity = suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        IdentityEntity[identityId]
                    }
                    require(authSession.id == identity.id.value) { "Not authorized in identity interceptor by identityId" }
                }

                "/api/v1/identities/update", "/api/v1/identities/change-password" -> {
                    val authSession = call.sessions.get(AuthSession::class)
                    requireNotNull(authSession) { "AuthSession is null in identity interceptor by identityRequest" }

                    val identityRequest = call.receive(IdentityRequest::class)
                    val identity = suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        IdentityEntity[identityRequest.id!!]
                    }
                    require(authSession.id == identity.id.value) {
                        "Not authorized in identity interceptor by identityRequest"
                    }
                }
            }
        }
    }

    routing {

        install(identityAuthenticationInterceptor)

        route("/api/v1/identities") {

            post("/create") {
                val identityRequest = call.receive(IdentityRequest::class)
                call.respond(HttpStatusCode.OK, identityService.create(identityRequest))
            }

            authenticate("auth-session-admin") {

                get {
                    call.respond(HttpStatusCode.OK, identityService.findAll())
                }

                put("/change-status") {
                    val identityRequest = call.receive(IdentityRequest::class)
                    identityService.changeStatus(identityRequest)
                    call.respond(HttpStatusCode.OK)
                }
            }

            authenticate("auth-session-all") {

                get("/by-id") {
                    val identityId = Uuid.parse(call.parameters["identityId"]!!)
                    call.respond(HttpStatusCode.OK, identityService.findById(identityId))
                }

                put("/update") {
                    val identityRequest = call.receive(IdentityRequest::class)
                    if (identityRequest.authority != null) {
                        call.sessions.clear(AuthSession::class)
                        val newAuthSession = AuthSession(identityRequest.id!!, identityRequest.authority)
                        call.sessions.set(newAuthSession, AuthSession::class)
                    }
                    call.respond(HttpStatusCode.OK, identityService.update(identityRequest))
                }

                post("/delete") {
                    val identityId = Uuid.parse(call.parameters["identityId"]!!)
                    identityService.delete(identityId)
                    call.respondRedirect("/api/v1/security/logout")
                }

                put("/change-password") {
                    val identityRequest = call.receive(IdentityRequest::class)
                    identityService.changePassword(identityRequest)
                    call.respond(HttpStatusCode.OK)
                }

                post("/upload-image") {
                    val identityId = Uuid.parse(call.parameters["identityId"]!!)
                    identityService.uploadImage(identityId, call.receiveMultipart(Long.MAX_VALUE))
                    call.respond(HttpStatusCode.OK)
                }

                delete("/remove-image") {
                    val identityId = Uuid.parse(call.parameters["identityId"]!!)
                    identityService.removeImage(identityId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}