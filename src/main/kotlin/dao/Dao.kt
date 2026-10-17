package org.burgas.dao

import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.io.readByteArray
import org.burgas.database.*
import org.burgas.dto.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import kotlin.uuid.Uuid

interface File

interface Dao

interface Uploader {
    fun upload(fileItem: PartData.FileItem)
}

interface Creator<in R : Request> {
    fun create(request: R)
}

interface Modifier<in R : Request> {
    fun update(request: R)
}

interface DependencyMapper<out D : Dependency> {
    suspend fun toDependency(): D
}

interface ResponseMapper<out R : Response> {
    suspend fun toResponse(): R
}

class ImageEntity(id: EntityID<Uuid>) : UuidEntity(id), File, Uploader, DependencyMapper<ImageDependency> {
    companion object : UuidEntityClass<ImageEntity>(ImageTable)

    var name by ImageTable.name
    var contentType by ImageTable.contentType
    var data by ImageTable.data
    var createdAt by ImageTable.createdAt

    @OptIn(InternalAPI::class)
    override fun upload(fileItem: PartData.FileItem) {
        name = fileItem.originalFileName
        contentType = "${fileItem.contentType!!.contentType}/${fileItem.contentType!!.contentSubtype}"
        data = ExposedBlob(fileItem.provider().readBuffer.readByteArray())
        createdAt = LocalDateTime.now().toKotlinLocalDateTime()
    }

    override suspend fun toDependency(): ImageDependency {
        return ImageDependency(id.value, name, contentType, createdAt)
    }
}

class IdentityEntity(id: EntityID<Uuid>) : UuidEntity(id), Dao, Creator<IdentityRequest>, Modifier<IdentityRequest>,
    DependencyMapper<IdentityDependency>, ResponseMapper<IdentityResponse> {
    companion object : UuidEntityClass<IdentityEntity>(IdentityTable)

    var authority by IdentityTable.authority
    var email by IdentityTable.email
    var password by IdentityTable.password
    var status by IdentityTable.status
    var firstname by IdentityTable.firstname
    var lastname by IdentityTable.lastname
    var patronymic by IdentityTable.patronymic
    var image by ImageEntity.optionalReferencedOn(IdentityTable.imageId)
    var createdAt by IdentityTable.createdAt
    var devices by DeviceEntity.via(IdentityDeviceTable.identityId, IdentityDeviceTable.deviceId)

    override fun create(request: IdentityRequest) {
        request.authority!!.let { authority = it }
        request.email.takeIf { !it.isNullOrEmpty() }!!.let { email = it }
        request.password.takeIf { !it.isNullOrEmpty() }!!.let { password = BCrypt.hashpw(it, BCrypt.gensalt()) }
        request.status?.let { status = it }
        request.firstname.takeIf { !it.isNullOrEmpty() }!!.let { firstname = it }
        request.lastname.takeIf { !it.isNullOrEmpty() }!!.let { lastname = it }
        request.patronymic.takeIf { !it.isNullOrEmpty() }!!.let { patronymic = it }
        createdAt = LocalDateTime.now().toKotlinLocalDateTime()
    }

    override fun update(request: IdentityRequest) {
        request.authority?.let { authority = it }
        request.email.takeIf { !it.isNullOrEmpty() }?.let { email = it }
        request.firstname.takeIf { !it.isNullOrEmpty() }?.let { firstname = it }
        request.lastname.takeIf { !it.isNullOrEmpty() }?.let { lastname = it }
        request.patronymic.takeIf { !it.isNullOrEmpty() }?.let { patronymic = it }
    }

    override suspend fun toDependency(): IdentityDependency {
        return IdentityDependency(
            id.value, authority, email, status, firstname, lastname, patronymic, image?.toDependency(), createdAt
        )
    }

    override suspend fun toResponse(): IdentityResponse {
        return IdentityResponse(
            id.value, authority, email, status, firstname, lastname, patronymic, image?.toDependency(), createdAt,
            devices.map { it.toDependency() }
        )
    }
}

class CategoryEntity(id: EntityID<Uuid>) : UuidEntity(id), Dao, Creator<CategoryRequest>, Modifier<CategoryRequest>,
    DependencyMapper<CategoryDependency>, ResponseMapper<CategoryResponse> {
    companion object : UuidEntityClass<CategoryEntity>(CategoryTable)

    var name by CategoryTable.name
    var description by CategoryTable.description
    var createdAt by CategoryTable.createdAt
    val devices by DeviceEntity.optionalReferrersOn(DeviceTable.categoryId)

    override fun create(request: CategoryRequest) {
        request.name.takeIf { !it.isNullOrEmpty() }!!.let { name = it }
        request.description.takeIf { !it.isNullOrEmpty() }!!.let { description = it }
        createdAt = LocalDateTime.now().toKotlinLocalDateTime()
    }

    override fun update(request: CategoryRequest) {
        request.name.takeIf { !it.isNullOrEmpty() }?.let { name = it }
        request.description.takeIf { !it.isNullOrEmpty() }?.let { description = it }
    }

    override suspend fun toDependency(): CategoryDependency {
        return CategoryDependency(id.value, name, description, createdAt)
    }

    override suspend fun toResponse(): CategoryResponse {
        return CategoryResponse(id.value, name, description, createdAt, devices.map { it.toDependency() })
    }

    suspend fun toResponse(identityId: Uuid): CategoryResponse {
        return CategoryResponse(
            id.value, name, description, createdAt,
            devices.filter { deviceEntity -> deviceEntity.identities.map { it.id.value }.contains(identityId) }
                .map { it.toDependency() }
        )
    }
}

class BrandEntity(id: EntityID<Uuid>) : UuidEntity(id), Dao, Creator<BrandRequest>, Modifier<BrandRequest>,
    DependencyMapper<BrandDependency>, ResponseMapper<BrandResponse> {
    companion object : UuidEntityClass<BrandEntity>(BrandTable)

    var name by BrandTable.name
    var description by BrandTable.description
    var createdAt by BrandTable.createdAt
    val devices by DeviceEntity.optionalReferrersOn(DeviceTable.brandId)

    override fun create(request: BrandRequest) {
        request.name.takeIf { !it.isNullOrEmpty() }!!.let { name = it }
        request.description.takeIf { !it.isNullOrEmpty() }!!.let { description = it }
        createdAt = LocalDateTime.now().toKotlinLocalDateTime()
    }

    override fun update(request: BrandRequest) {
        request.name.takeIf { !it.isNullOrEmpty() }?.let { name = it }
        request.description.takeIf { !it.isNullOrEmpty() }?.let { description = it }
    }

    override suspend fun toDependency(): BrandDependency {
        return BrandDependency(id.value, name, description, createdAt)
    }

    override suspend fun toResponse(): BrandResponse {
        return BrandResponse(id.value, name, description, createdAt, devices.map { it.toDependency() })
    }

    suspend fun toResponse(identityId: Uuid): BrandResponse {
        return BrandResponse(
            id.value, name, description, createdAt,
            devices.filter { deviceEntity -> deviceEntity.identities.map { it.id.value }.contains(identityId) }
                .map { it.toDependency() }
        )
    }
}

class DeviceEntity(id: EntityID<Uuid>) : UuidEntity(id), Dao, Creator<DeviceRequest>, Modifier<DeviceRequest>,
    DependencyMapper<DeviceDependency>, ResponseMapper<DeviceResponse> {
    companion object : UuidEntityClass<DeviceEntity>(DeviceTable)

    var category by CategoryEntity.optionalReferencedOn(DeviceTable.categoryId)
    var brand by BrandEntity.optionalReferencedOn(DeviceTable.brandId)
    var model by DeviceTable.model
    var specifications by DeviceTable.specifications
    var inProgress by DeviceTable.inProgress
    var ipAddress by DeviceTable.ipAddress
    var port by DeviceTable.port
    var token by DeviceTable.token
    var image by ImageEntity.optionalReferencedOn(DeviceTable.imageId)
    var createdAt by DeviceTable.createdAt
    var identities by IdentityEntity.via(IdentityDeviceTable.deviceId, IdentityDeviceTable.identityId)

    override fun create(request: DeviceRequest) {
        request.categoryId!!.let { category = CategoryEntity[it] }
        request.brandId!!.let { brand = BrandEntity[it] }
        request.model.takeIf { !it.isNullOrEmpty() }!!.let { model = it }
        request.specifications.takeIf { !it.isNullOrEmpty() }!!.let { specifications = it }
        request.inProgress?.let { inProgress = it }
        request.ipAddress.takeIf { !it.isNullOrEmpty() }?.let { ipAddress = it }
        request.port?.let { port = it }
        request.token.takeIf { !it.isNullOrEmpty() }?.let { token = it }
        createdAt = LocalDateTime.now().toKotlinLocalDateTime()
    }

    override fun update(request: DeviceRequest) {
        request.categoryId?.let { category = CategoryEntity[it] }
        request.brandId?.let { brand = BrandEntity[it] }
        request.model.takeIf { !it.isNullOrEmpty() }?.let { model = it }
        request.specifications.takeIf { !it.isNullOrEmpty() }?.let { specifications = it }
        request.inProgress?.let { inProgress = it }
        request.ipAddress.takeIf { !it.isNullOrEmpty() }?.let { ipAddress = it }
        request.port?.let { port = it }
        request.token.takeIf { !it.isNullOrEmpty() }?.let { token = it }
    }

    override suspend fun toDependency(): DeviceDependency {
        return DeviceDependency(
            id.value, category?.toDependency(), brand?.toDependency(),
            model, specifications, inProgress, ipAddress, port, token, image?.toDependency(), createdAt
        )
    }

    override suspend fun toResponse(): DeviceResponse {
        return DeviceResponse(
            id.value,
            category?.toDependency(),
            brand?.toDependency(),
            model,
            specifications,
            inProgress,
            ipAddress,
            port,
            token,
            image?.toDependency(),
            createdAt,
            identities.map { it.toDependency() }
        )
    }
}