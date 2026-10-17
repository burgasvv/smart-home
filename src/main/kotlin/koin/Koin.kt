package org.burgas.koin

import io.ktor.server.application.*
import org.burgas.service.BrandService
import org.burgas.service.CategoryService
import org.burgas.service.DeviceService
import org.burgas.service.IdentityService
import org.burgas.service.ImageService
import org.burgas.service.XiaomiYeelightService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureKoin() {
    val module = module {
        singleOf(::ImageService)
        single {
            IdentityService(
                imageService = get<ImageService>(),
                deviceService = get<DeviceService>()
            )
        }
        singleOf(::CategoryService)
        singleOf(::BrandService)
        singleOf(::DeviceService)
        singleOf(::XiaomiYeelightService)
    }
    install(Koin) {
        modules(module)
    }
}