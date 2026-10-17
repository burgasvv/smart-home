package org.burgas.router

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.JsonObject
import org.burgas.service.DeviceService
import org.burgas.service.XiaomiYeelightService
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Application.configureXiaomiRouter() {

    val xiaomiService by inject<XiaomiYeelightService>()
    val deviceService by inject<DeviceService>()

    routing {

        route("/api/v1/xiaomi") {

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
        }
    }
}