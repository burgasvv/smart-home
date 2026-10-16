package org.burgas

import io.ktor.server.application.*
import org.burgas.database.configureDatabase
import org.burgas.koin.configureKoin
import org.burgas.router.*
import org.burgas.security.configureSecurity
import org.burgas.serialization.configureSerialization

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.modules() {
    configureDatabase()
    configureSerialization()
    configureSecurity()
    configureKoin()

    configureSecurityRouter()
    configureImageRouter()
    configureIdentityRouter()
    configureCategoryRouter()
    configureBrandRouter()
    configureDeviceRouter()
}
