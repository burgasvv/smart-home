package org.burgas.router

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import org.burgas.dao.DeviceEntity
import org.burgas.dao.IdentityEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.AuthSession
import org.burgas.dto.DeviceRequest
import org.burgas.service.DeviceService
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureDeviceRouter() {

    val deviceService by inject<DeviceService>()

    val deviceAuthenticationInterceptor = createRouteScopedPlugin("DeviceAuthenticationInterceptor") {
        on(AuthenticationChecked) { call ->
            when(call.request.path()) {

                "/api/v1/devices/by-id", "/api/v1/devices/delete" -> {
                    val authSession = call.sessions.get(AuthSession::class)
                    requireNotNull(authSession) { "Not authenticated in device interceptor by deviceId" }

                    val deviceId = Uuid.parse(call.parameters["deviceId"]!!)
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val deviceEntity = DeviceEntity[deviceId]
                        require(deviceEntity.identities.map { it.id.value }.contains(authSession.id)) {
                            "Not authorized in device interceptor by deviceId"
                        }
                    }
                }

                "/api/v1/devices/create" -> {
                    val authSession = call.sessions.get(AuthSession::class)
                    requireNotNull(authSession) { "Not authenticated in device interceptor by create" }

                    val deviceRequest = call.receive(DeviceRequest::class)
                    val identity = suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        IdentityEntity[deviceRequest.identityId!!]
                    }
                    require(identity.id.value == authSession.id) { "Not authorized in device interceptor by create" }
                }

                "/api/v1/devices/update" -> {
                    val authSession = call.sessions.get(AuthSession::class)
                    requireNotNull(authSession) { "Not authenticated in device interceptor by update" }

                    val deviceRequest = call.receive(DeviceRequest::class)
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val deviceEntity = DeviceEntity[deviceRequest.id!!]
                        require(deviceEntity.identities.map { it.id.value }.contains(authSession.id)) {
                            "Not authorized in device interceptor by update"
                        }
                    }
                }
            }
        }
    }

    routing {

        install(deviceAuthenticationInterceptor)

        route("/api/v1/devices") {

            authenticate("auth-session-all") {

                get("/by-id") {
                    val deviceId = Uuid.parse(call.parameters["deviceId"]!!)
                    call.respond(HttpStatusCode.OK, deviceService.findById(deviceId))
                }

                post("/create") {
                    val deviceRequest = call.receive(DeviceRequest::class)
                    call.respond(HttpStatusCode.OK, deviceService.create(deviceRequest))
                }

                put("/update") {
                    val deviceRequest = call.receive(DeviceRequest::class)
                    call.respond(HttpStatusCode.OK, deviceService.update(deviceRequest))
                }

                delete("/delete") {
                    val deviceId = Uuid.parse(call.parameters["deviceId"]!!)
                    deviceService.delete(deviceId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}