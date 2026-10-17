package org.burgas.service

import kotlinx.serialization.json.*
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

    suspend fun setBright(ipAddress: String, token: String, jsonObject: JsonObject) {
        val bright = jsonObject["bright"]!!.jsonPrimitive.int
        val type = jsonObject["type"]?.jsonPrimitive?.content
        val strength = jsonObject["strength"]?.jsonPrimitive?.int
        XiaomiMiIoClient.sendCommand(ipAddress, token) {
            put("id", 1)
            put("method", "set_bright")
            put("params", buildJsonArray {
                add(bright)
                add(type)
                add(strength)
            })
        }
    }

    suspend fun setTemperature(ipAddress: String, token: String, jsonObject: JsonObject) {
        val temperature = jsonObject["temperature"]!!.jsonPrimitive.int
        val type = jsonObject["type"]?.jsonPrimitive?.content
        val strength = jsonObject["strength"]?.jsonPrimitive?.int
        XiaomiMiIoClient.sendCommand(ipAddress, token) {
            put("id", 1)
            put("method", "set_ct_abx")
            put("params", buildJsonArray {
                add(temperature)
                add(type)
                add(strength)
            })
        }
    }

    suspend fun setRgb(ipAddress: String, token: String, jsonObject: JsonObject) {
        val color = jsonObject["color"]!!.jsonPrimitive.int
        val type = jsonObject["type"]?.jsonPrimitive?.content
        val strength = jsonObject["strength"]?.jsonPrimitive?.int
        XiaomiMiIoClient.sendCommand(ipAddress, token) {
            put("id", 1)
            put("method", "set_rgb")
            put("params", buildJsonArray {
                add(color)
                add(type)
                add(strength)
            })
        }
    }
}