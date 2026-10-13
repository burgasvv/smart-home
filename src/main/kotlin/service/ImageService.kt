package org.burgas.service

import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import org.burgas.dao.ImageEntity
import org.burgas.database.DatabaseConnection
import org.burgas.service.contract.DeleteService
import org.burgas.service.contract.ReadService
import org.burgas.service.contract.UploadService
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.sql.Connection
import kotlin.uuid.Uuid

class ImageService : ReadService<Uuid, ImageEntity>, UploadService<ImageEntity>, DeleteService<Uuid> {

    override suspend fun readEntity(id: Uuid): ImageEntity = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        ImageEntity[id]
    }

    override suspend fun upload(multiPartData: MultiPartData): ImageEntity = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val fileItem = multiPartData.asFlow().filterIsInstance(PartData.FileItem::class).first()
        ImageEntity.new { this.upload(fileItem) }
    }

    override suspend fun delete(id: Uuid) {
        ImageEntity[id].delete()
    }
}