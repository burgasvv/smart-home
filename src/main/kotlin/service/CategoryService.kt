package org.burgas.service

import org.burgas.dao.CategoryEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.CategoryRequest
import org.burgas.dto.CategoryResponse
import org.burgas.service.contract.ReadService
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.sql.Connection
import kotlin.uuid.Uuid

class CategoryService : ReadService<Uuid, CategoryEntity> {

    suspend fun findAllByIdentity(identityId: Uuid): List<CategoryResponse> = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        CategoryEntity.all().with(CategoryEntity::devices).map { it.toResponse(identityId) }
    }

    override suspend fun readEntity(id: Uuid): CategoryEntity = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        CategoryEntity[id].load(CategoryEntity::devices)
    }

    suspend fun findByIdAndIdentityId(categoryId: Uuid, identityId: Uuid): CategoryResponse = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        readEntity(categoryId).toResponse(identityId)
    }

    suspend fun createWithIdentityId(req: CategoryRequest, identityId: Uuid): CategoryResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        CategoryEntity.new { this.create(req) }.load(CategoryEntity::devices).toResponse(identityId)
    }

    suspend fun updateWithIdentityId(req: CategoryRequest, identityId: Uuid): CategoryResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        CategoryEntity.findByIdAndUpdate(req.id!!) { it.update(req) }!!.load(CategoryEntity::devices)
            .toResponse(identityId)
    }
}