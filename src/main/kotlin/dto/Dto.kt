package org.burgas.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.burgas.database.Authority
import kotlin.uuid.Uuid

interface Request {
    val id: Uuid?
}

interface Response {
    val id: Uuid
}

interface Dependency {
    val id: Uuid
}

@Serializable
data class ExceptionResponse(
    val status: String,
    val code: Int,
    val message: String?
)

@Serializable
data class AuthSession(
    val id: Uuid,
    val authority: Authority
)

@Serializable
data class AuthData(
    val email: String,
    val password: String
)

@Serializable
data class ImageDependency(
    override val id: Uuid,
    val name: String?,
    val contentType: String,
    val createdAt: LocalDateTime
) : Dependency

@Serializable
data class IdentityRequest(
    override val id: Uuid?,
    val authority: Authority?,
    val email: String?,
    val password: String?,
    val status: Boolean?,
    val firstname: String?,
    val lastname: String?,
    val patronymic: String?
) : Request

@Serializable
data class IdentityDependency(
    override val id: Uuid,
    val authority: Authority,
    val email: String,
    val status: Boolean,
    val firstname: String,
    val lastname: String,
    val patronymic: String,
    val image: ImageDependency?,
    val createdAt: LocalDateTime
) : Dependency

@Serializable
data class IdentityResponse(
    override val id: Uuid,
    val authority: Authority,
    val email: String,
    val status: Boolean,
    val firstname: String,
    val lastname: String,
    val patronymic: String,
    val image: ImageDependency?,
    val createdAt: LocalDateTime,
    val devices: List<DeviceDependency>
) : Response

@Serializable
data class CategoryRequest(
    override val id: Uuid?,
    val name: String?,
    val description: String?
) : Request

@Serializable
data class CategoryDependency(
    override val id: Uuid,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime
) : Dependency

@Serializable
data class CategoryResponse(
    override val id: Uuid,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime,
    val devices: List<DeviceDependency>?
) : Response

@Serializable
data class BrandRequest(
    override val id: Uuid?,
    val name: String?,
    val description: String?
) : Request

@Serializable
data class BrandDependency(
    override val id: Uuid,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime
) : Dependency

@Serializable
data class BrandResponse(
    override val id: Uuid,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime,
    val devices: List<DeviceDependency>?
) : Response

@Serializable
data class DeviceRequest(
    override val id: Uuid?,
    val categoryId: Uuid?,
    val brandId: Uuid?,
    val identityId: Uuid?,
    val model: String?,
    val specifications: String?,
    val inProgress: Boolean?,
    val ipAddress: String?,
    val port: Int?,
    val token: String?
) : Request

@Serializable
data class DeviceDependency(
    override val id: Uuid,
    val category: CategoryDependency?,
    val brand: BrandDependency?,
    val model: String,
    val specifications: String,
    val inProgress: Boolean,
    val ipAddress: String?,
    val port: Int?,
    val token: String?,
    val image: ImageDependency?,
    val createdAt: LocalDateTime
) : Dependency

@Serializable
data class DeviceResponse(
    override val id: Uuid,
    val category: CategoryDependency?,
    val brand: BrandDependency?,
    val model: String,
    val specifications: String,
    val inProgress: Boolean,
    val ipAddress: String?,
    val port: Int?,
    val token: String?,
    val image: ImageDependency?,
    val createdAt: LocalDateTime,
    val identities: List<IdentityDependency>
) : Response