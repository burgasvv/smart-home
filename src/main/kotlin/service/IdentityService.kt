package org.burgas.service

import io.ktor.http.content.MultiPartData
import org.burgas.dao.IdentityEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.IdentityRequest
import org.burgas.dto.IdentityResponse
import org.burgas.redis.RedisCacheHandler
import org.burgas.redis.RedisKeys
import org.burgas.serialization.appJson
import org.burgas.service.contract.CollectService
import org.burgas.service.contract.CreateService
import org.burgas.service.contract.DeleteService
import org.burgas.service.contract.FindService
import org.burgas.service.contract.ReadService
import org.burgas.service.contract.UpdateService
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import kotlin.uuid.Uuid

class IdentityService : RedisCacheHandler<IdentityResponse>, CollectService<IdentityResponse>,
    ReadService<Uuid, IdentityEntity>, FindService<Uuid, IdentityResponse>,
    CreateService<IdentityRequest, IdentityResponse>,
    UpdateService<IdentityRequest, IdentityResponse>, DeleteService<Uuid> {

    private val redis = DatabaseConnection.redis
    private val imageService: ImageService

    constructor(imageService: ImageService) {
        this.imageService = imageService
    }

    override suspend fun handleCache(response: IdentityResponse) {
        val identityKey = RedisKeys.IDENTITY_KEY.format(response.id)
        redis.del(identityKey)
        response.devices.forEach { deviceDependency ->
            val deviceKey = RedisKeys.DEVICE_KEY.format(deviceDependency.id)
            redis.del(deviceKey)
        }
    }

    override suspend fun findAll(): List<IdentityResponse> = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        IdentityEntity.all()
            .with(IdentityEntity::image, IdentityEntity::devices)
            .map { it.toResponse() }
    }

    override suspend fun readEntity(id: Uuid): IdentityEntity = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        IdentityEntity[id].load(IdentityEntity::image, IdentityEntity::devices)
    }

    override suspend fun findById(id: Uuid): IdentityResponse = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        val identityKey = RedisKeys.IDENTITY_KEY.format(id)
        if (redis.exists(identityKey)) {
            appJson.decodeFromString<IdentityResponse>(redis[identityKey])
        } else {
            val identityResponse = readEntity(id).toResponse()
            redis.set(identityKey, appJson.encodeToString(identityResponse))
            identityResponse
        }
    }

    override suspend fun create(req: IdentityRequest): IdentityResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityResponse = IdentityEntity.new { this.create(req) }
            .load(IdentityEntity::image, IdentityEntity::devices).toResponse()
        handleCache(identityResponse)
        val identityKey = RedisKeys.IDENTITY_KEY.format(identityResponse.id)
        redis.set(identityKey, appJson.encodeToString(identityResponse))
        identityResponse
    }

    override suspend fun update(req: IdentityRequest): IdentityResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityResponse = IdentityEntity.findByIdAndUpdate(req.id!!) { it.update(req) }!!
            .load(IdentityEntity::image, IdentityEntity::devices).toResponse()
        handleCache(identityResponse)
        val identityKey = RedisKeys.IDENTITY_KEY.format(identityResponse.id)
        redis.set(identityKey, appJson.encodeToString(identityResponse))
        identityResponse
    }

    override suspend fun delete(id: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityEntity = readEntity(id)
        handleCache(identityEntity.toResponse())
        identityEntity.delete()
    }

    suspend fun changePassword(request: IdentityRequest) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityEntity = readEntity(request.id!!)
        require(!BCrypt.checkpw(request.password!!, identityEntity.password)) { "Old and new passwords matched" }
        identityEntity.password = BCrypt.hashpw(request.password, BCrypt.gensalt())
        handleCache(identityEntity.toResponse())
    }

    suspend fun changeStatus(request: IdentityRequest) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityEntity = readEntity(request.id!!)
        require(identityEntity.status != request.status!!) { "Old and new statuses matched" }
        identityEntity.status = request.status
        handleCache(identityEntity.toResponse())
    }

    suspend fun uploadImage(id: Uuid, multiPartData: MultiPartData) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityEntity = readEntity(id)
        require(identityEntity.image == null) { "Identity image already set" }
        identityEntity.image = imageService.upload(multiPartData)
        handleCache(identityEntity.toResponse())
    }

    suspend fun removeImage(id: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityEntity = readEntity(id)
        identityEntity.image!!.delete()
        handleCache(identityEntity.toResponse())
    }
}