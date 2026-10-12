package org.burgas.dao

import org.burgas.database.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

class ImageEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<ImageEntity>(ImageTable)

    var name by ImageTable.name
    var contentType by ImageTable.contentType
    var data by ImageTable.data
    var createdAt by ImageTable.createdAt
}

class IdentityEntity(id: EntityID<Uuid>) : UuidEntity(id) {
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
}

class CategoryEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<CategoryEntity>(CategoryTable)

    var name by CategoryTable.name
    var description by CategoryTable.description
    var createdAt by CategoryTable.createdAt
    val devices by DeviceEntity.optionalReferrersOn(DeviceTable.categoryId)
}

class BrandEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<BrandEntity>(BrandTable)

    var name by BrandTable.name
    var description by BrandTable.description
    var createdAt by BrandTable.createdAt
    val devices by DeviceEntity.optionalReferrersOn(DeviceTable.brandId)
}

class DeviceEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<DeviceEntity>(DeviceTable)

    var category by CategoryEntity.optionalReferencedOn(DeviceTable.categoryId)
    var brand by BrandEntity.optionalReferencedOn(DeviceTable.brandId)
    var model by DeviceTable.model
    var specifications by DeviceTable.specifications
    var inProgress by DeviceTable.inProgress
    var image by ImageEntity.optionalReferencedOn(DeviceTable.imageId)
    var createdAt by DeviceTable.createdAt
    var identities by IdentityEntity.via(IdentityDeviceTable.deviceId, IdentityDeviceTable.identityId)
}