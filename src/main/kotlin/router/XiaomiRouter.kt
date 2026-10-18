package org.burgas.router

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.AuthenticationProcedureChallenge
import io.ktor.server.auth.authenticate
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.serialization.json.JsonObject
import org.burgas.dao.DeviceEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.AuthSession
import org.burgas.service.DeviceService
import org.burgas.service.XiaomiYeelightService
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureXiaomiRouter() {

    val xiaomiService by inject<XiaomiYeelightService>()
    val deviceService by inject<DeviceService>()

    val xiaomiAuthenticationInterceptor = createRouteScopedPlugin("XiaomiAuthenticationInterceptor") {
        on(AuthenticationChecked) { call ->

            when (call.request.path()) {

                "/api/v1/xiaomi/power", "/api/v1/xiaomi/toggle", "/api/v1/xiaomi/bright",
                "/api/v1/xiaomi/temperature", "/api/v1/xiaomi/rgb" -> {

                    val authSession = call.sessions.get(AuthSession::class)
                    requireNotNull(authSession) { "Not authenticated in xiaomi interceptor by deviceId" }

                    val deviceId = Uuid.parse(call.parameters["deviceId"]!!)
                    suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        val deviceEntity = DeviceEntity[deviceId]

                        require(deviceEntity.identities.map { it.id.value }.contains(authSession.id)) {
                            "Not authorized in xiaomi interceptor by deviceId"
                        }
                    }
                }
            }
        }
    }

    routing {

        install(xiaomiAuthenticationInterceptor)

        route("/api/v1/xiaomi") {

            authenticate("auth-session-all") {

                post("/power") {
                    val deviceId = Uuid.parse(call.parameters["deviceId"]!!)
                    val deviceResponse = deviceService.findById(deviceId)
                    val jsonObject = call.receive<JsonObject>()
                    xiaomiService.setPower(deviceResponse.ipAddress!!, deviceResponse.token!!, jsonObject)
                    call.respond(HttpStatusCode.OK)
                }

                post("/toggle") {
                    val deviceId = Uuid.parse(call.parameters["deviceId"]!!)
                    val deviceResponse = deviceService.findById(deviceId)
                    xiaomiService.toggle(deviceResponse.ipAddress!!, deviceResponse.token!!)
                    call.respond(HttpStatusCode.OK)
                }

                post("/bright") {
                    val deviceId = Uuid.parse(call.parameters["deviceId"]!!)
                    val deviceResponse = deviceService.findById(deviceId)
                    val jsonObject = call.receive<JsonObject>()
                    xiaomiService.setBright(deviceResponse.ipAddress!!, deviceResponse.token!!, jsonObject)
                    call.respond(HttpStatusCode.OK)
                }

                post("/temperature") {
                    val deviceId = Uuid.parse(call.parameters["deviceId"]!!)
                    val deviceResponse = deviceService.findById(deviceId)
                    val jsonObject = call.receive<JsonObject>()
                    xiaomiService.setTemperature(deviceResponse.ipAddress!!, deviceResponse.token!!, jsonObject)
                    call.respond(HttpStatusCode.OK)
                }

                post("/rgb") {
                    val deviceId = Uuid.parse(call.parameters["deviceId"]!!)
                    val deviceResponse = deviceService.findById(deviceId)
                    val jsonObject = call.receive<JsonObject>()
                    xiaomiService.setRgb(deviceResponse.ipAddress!!, deviceResponse.token!!, jsonObject)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}