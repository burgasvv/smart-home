package org.burgas.service

import io.ktor.http.content.MultiPartData
import org.burgas.dao.DeviceEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.DeviceRequest
import org.burgas.dto.DeviceResponse
import org.burgas.redis.RedisCacheHandler
import org.burgas.redis.RedisKeys
import org.burgas.serialization.appJson
import org.burgas.service.contract.*
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.sql.Connection
import kotlin.uuid.Uuid

class DeviceService : RedisCacheHandler<DeviceResponse>, ReadService<Uuid, DeviceEntity>,
    FindService<Uuid, DeviceResponse>, CreateService<DeviceRequest, DeviceResponse>,
    UpdateService<DeviceRequest, DeviceResponse>, DeleteService<Uuid>, KoinComponent {

    private val redis = DatabaseConnection.redis
    private val identityService by inject<IdentityService>()
    private val imageService: ImageService

    constructor(imageService: ImageService) {
        this.imageService = imageService
    }

    override suspend fun handleCache(response: DeviceResponse) {
        val deviceKey = RedisKeys.DEVICE_KEY.format(response.id)
        redis.del(deviceKey)
        response.identities.forEach { identityDependency ->
            val identityKey = RedisKeys.IDENTITY_KEY.format(identityDependency.id)
            redis.del(identityKey)
        }
    }

    override suspend fun readEntity(id: Uuid): DeviceEntity = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        DeviceEntity[id].load(
            DeviceEntity::category, DeviceEntity::brand, DeviceEntity::image, DeviceEntity::identities
        )
    }

    override suspend fun findById(id: Uuid): DeviceResponse = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        val deviceKey = RedisKeys.DEVICE_KEY.format(id)
        if (redis.exists(deviceKey)) {
            appJson.decodeFromString<DeviceResponse>(redis[deviceKey])
        } else {
            val deviceResponse = readEntity(id).toResponse()
            redis.set(deviceKey, appJson.encodeToString(deviceResponse))
            deviceResponse
        }
    }

    override suspend fun create(req: DeviceRequest): DeviceResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val deviceEntity = DeviceEntity.new { this.create(req) }.load(
            DeviceEntity::category, DeviceEntity::brand, DeviceEntity::image, DeviceEntity::identities
        )
        val identityEntity = identityService.readEntity(req.identityId!!)
        identityEntity.devices = SizedCollection(identityEntity.devices + deviceEntity)
        val deviceResponse = deviceEntity.toResponse()
        handleCache(deviceResponse)
        deviceResponse
    }

    override suspend fun update(req: DeviceRequest): DeviceResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val deviceResponse = DeviceEntity.findByIdAndUpdate(req.id!!) { it.update(req) }!!.load(
            DeviceEntity::category, DeviceEntity::brand, DeviceEntity::image, DeviceEntity::identities
        ).toResponse()
        handleCache(deviceResponse)
        deviceResponse
    }

    override suspend fun delete(id: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val deviceEntity = readEntity(id)
        deviceEntity.image?.delete()
        deviceEntity.delete()
        handleCache(deviceEntity.toResponse())
    }

    suspend fun uploadImage(deviceId: Uuid, multiPartData: MultiPartData) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val deviceEntity = readEntity(deviceId)
        require(deviceEntity.image == null) { "Device image is already set" }
        deviceEntity.image = imageService.upload(multiPartData)
        handleCache(deviceEntity.toResponse())
    }

    suspend fun removeImage(deviceId: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val deviceEntity = readEntity(deviceId)
        deviceEntity.image!!.delete()
        handleCache(deviceEntity.toResponse())
    }
}