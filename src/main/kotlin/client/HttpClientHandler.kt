package org.burgas.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.json
import org.burgas.serialization.appJson

object HttpClientHandler {

    val httpClient: HttpClient = HttpClient(CIO) {
        install(WebSockets)
        install(ContentNegotiation) { json(appJson) }
    }
}