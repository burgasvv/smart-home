package org.burgas.serialization

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

val appJson = Json {
    allowStructuredMapKeys = true
    useArrayPolymorphism = true
    isLenient = true
    ignoreUnknownKeys = true
    prettyPrint = true
    allowComments = true
    allowSpecialFloatingPointValues = true
    allowTrailingComma = true
    explicitNulls = false
}

fun Application.configureSerialization() {
    install(ContentNegotiation) { json(appJson) }
}