package org.burgas.router

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.burgas.dto.AuthSession
import org.burgas.dto.CategoryRequest
import org.burgas.service.CategoryService
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureCategoryRouter() {

    val categoryService by inject<CategoryService>()

    routing {

        route("/api/v1/categories") {

            authenticate("auth-session-all") {

                get {
                    val authSession = call.sessions.get(AuthSession::class)!!
                    call.respond(HttpStatusCode.OK, categoryService.findAllByIdentity(authSession.id))
                }

                get("/by-id") {
                    val authSession = call.sessions.get(AuthSession::class)!!
                    val categoryId = Uuid.parse(call.parameters["categoryId"]!!)
                    call.respond(HttpStatusCode.OK, categoryService.findByIdAndIdentityId(categoryId, authSession.id))
                }
            }

            authenticate("auth-session-admin") {

                post("/create") {
                    val authSession = call.sessions.get(AuthSession::class)!!
                    val categoryRequest = call.receive(CategoryRequest::class)
                    call.respond(
                        HttpStatusCode.OK,
                        categoryService.createWithIdentityId(categoryRequest, authSession.id)
                    )
                }

                put("/update") {
                    val authSession = call.sessions.get(AuthSession::class)!!
                    val categoryRequest = call.receive(CategoryRequest::class)
                    call.respond(
                        HttpStatusCode.OK,
                        categoryService.updateWithIdentityId(categoryRequest, authSession.id)
                    )
                }
            }
        }
    }
}