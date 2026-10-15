package org.burgas.router

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.burgas.dto.AuthSession
import org.burgas.dto.BrandRequest
import org.burgas.service.BrandService
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureBrandRouter() {

    val brandService by inject<BrandService>()

    routing {

        route("/api/v1/brands") {

            authenticate("auth-session-all") {

                get {
                    val authSession = call.sessions.get(AuthSession::class)!!
                    call.respond(HttpStatusCode.OK, brandService.findAllByIdentity(authSession.id))
                }

                get("/by-id") {
                    val authSession = call.sessions.get(AuthSession::class)!!
                    val brandId = Uuid.parse(call.parameters["brandId"]!!)
                    call.respond(HttpStatusCode.OK, brandService.findByIdAndIdentityId(brandId, authSession.id))
                }
            }

            authenticate("auth-session-admin") {

                post("/create") {
                    val authSession = call.sessions.get(AuthSession::class)!!
                    val brandRequest = call.receive(BrandRequest::class)
                    call.respond(HttpStatusCode.OK, brandService.createWithIdentityId(brandRequest, authSession.id))
                }

                put("/update") {
                    val authSession = call.sessions.get(AuthSession::class)!!
                    val brandRequest = call.receive(BrandRequest::class)
                    call.respond(HttpStatusCode.OK, brandService.updateWithIdentityId(brandRequest, authSession.id))
                }

                delete("/delete") {
                    val brandId = Uuid.parse(call.parameters["brandId"]!!)
                    brandService.delete(brandId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}