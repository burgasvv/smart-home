package org.burgas.koin

import io.ktor.server.application.*
import org.burgas.service.IdentityService
import org.burgas.service.ImageService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureKoin() {
    val module = module {
        singleOf(::ImageService)
        single { IdentityService(imageService = get<ImageService>()) }
    }
    install(Koin) {
        modules(module)
    }
}