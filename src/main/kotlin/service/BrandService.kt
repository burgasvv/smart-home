package org.burgas.service

import org.burgas.dao.BrandEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.BrandRequest
import org.burgas.service.contract.ReadService
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.sql.Connection
import kotlin.uuid.Uuid

class BrandService : ReadService<Uuid, BrandEntity> {

    suspend fun findAllByIdentity(identityId: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        BrandEntity.all().with(BrandEntity::devices).map { it.toResponse(identityId) }
    }

    override suspend fun readEntity(id: Uuid): BrandEntity = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        BrandEntity[id].load(BrandEntity::devices)
    }

    suspend fun findByIdAndIdentityId(brandId: Uuid, identityId: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        readEntity(brandId).toResponse(identityId)
    }

    suspend fun createWithIdentityId(request: BrandRequest, identityId: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        BrandEntity.new { this.create(request) }.load(BrandEntity::devices).toResponse(identityId)
    }

    suspend fun updateWithIdentityId(request: BrandRequest, identityId: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        BrandEntity.findByIdAndUpdate(request.id!!) { it.update(request) }!!
            .load(BrandEntity::devices).toResponse(identityId)
    }

    suspend fun delete(brandId: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        readEntity(brandId).delete()
    }
}