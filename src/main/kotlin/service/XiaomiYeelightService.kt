package org.burgas.service

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.burgas.client.XiaomiMiIoClient

class XiaomiYeelightService {

    suspend fun setPower(ipAddress: String, token: String, jsonObject: JsonObject) {
        val power = jsonObject["power"]!!.jsonPrimitive.boolean
        val type = jsonObject["type"]?.jsonPrimitive?.content
        val strength = jsonObject["strength"]?.jsonPrimitive?.int
        XiaomiMiIoClient.sendCommand(ipAddress, token) {
            put("id", 1)
            put("method", "set_power")
            put("params", buildJsonArray {
                add(if (power) "on" else "off")
                add(type)
                add(strength)
            })
        }
    }

    suspend fun toggle(ipAddress: String, token: String) {
        XiaomiMiIoClient.sendCommand(ipAddress, token) {
            put("id", 1)
            put("method", "toggle")
            put("params", buildJsonArray {  })
        }
    }
}