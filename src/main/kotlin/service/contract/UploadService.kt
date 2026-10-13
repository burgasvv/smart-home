package org.burgas.service.contract

import io.ktor.http.content.MultiPartData
import org.burgas.dao.File

interface UploadService<F : File> {

    suspend fun upload(multiPartData: MultiPartData): F
}