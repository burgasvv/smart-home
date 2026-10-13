package org.burgas.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.config.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.Database
import redis.clients.jedis.Jedis

object DatabaseConnection {

    private val config = ApplicationConfig("application.yaml")

    private val hikariConfig: HikariConfig = HikariConfig()

    init {
        hikariConfig.driverClassName = "org.postgresql.Driver"
        hikariConfig.jdbcUrl = config.property("postgres.url").getString()
        hikariConfig.username = config.property("postgres.user").getString()
        hikariConfig.password = config.property("postgres.password").getString()
        hikariConfig.minimumIdle = 5
        hikariConfig.maximumPoolSize = 50
        hikariConfig.isAutoCommit = false
        hikariConfig.validate()
    }

    val dataSource: HikariDataSource = HikariDataSource(hikariConfig)

    val postgres = Database.connect(
        datasource = dataSource, databaseConfig = DatabaseConfig { this.explicitDialect = PostgreSQLDialect() }
    )

    val redis = Jedis(
        config.property("redis.host").getString(),
        config.property("redis.port").getString().toInt()
    )
}

fun runMigration(dataSource: HikariDataSource) {
    val flyway = Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .locations("classpath:db/migration")
        .load()
    flyway.migrate()
}

fun Application.configureDatabase() {
    val runMigrationPlugin = createApplicationPlugin("RunMigrationPlugin") {
        runMigration(DatabaseConnection.dataSource)
    }
    install(runMigrationPlugin)
}

object ImageTable : UuidTable("image") {
    val name = varchar("name", 250).nullable()
    val contentType = varchar("content_type", 250)
    val data = blob("data")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

enum class Authority {
    ADMIN, USER
}

object IdentityTable : UuidTable("identity") {
    val authority = enumerationByName<Authority>("authority", 250)
    val email = varchar("email", 250).uniqueIndex()
    val password = varchar("password", 250)
    val status = bool("status").default(true)
    val firstname = varchar("firstname", 250)
    val lastname = varchar("lastname", 250)
    val patronymic = varchar("patronymic", 250)
    val imageId = optReference(
        name = "image_id", refColumn = ImageTable.id,
        onDelete = ReferenceOption.SET_NULL, onUpdate = ReferenceOption.CASCADE
    )
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object CategoryTable : UuidTable("category") {
    val name = varchar("name", 250).uniqueIndex()
    val description = text("description").uniqueIndex()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object BrandTable : UuidTable("brand") {
    val name = varchar("name", 250).uniqueIndex()
    val description = text("description").uniqueIndex()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object DeviceTable : UuidTable("device") {
    val categoryId = optReference(
        name = "category_id", refColumn = CategoryTable.id,
        onDelete = ReferenceOption.SET_NULL, onUpdate = ReferenceOption.CASCADE
    )
    val brandId = optReference(
        name = "brand_id", refColumn = BrandTable.id,
        onDelete = ReferenceOption.SET_NULL, onUpdate = ReferenceOption.CASCADE
    )
    val model = varchar("model", 250).uniqueIndex()
    val specifications = text("specifications")
    val inProgress = bool("in_progress").default(true)
    val imageId = optReference(
        name = "image_id", refColumn = ImageTable.id,
        onDelete = ReferenceOption.SET_NULL, onUpdate = ReferenceOption.CASCADE
    )
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

object IdentityDeviceTable : Table("identity_device") {
    val identityId = reference(
        name = "identity_id", refColumn = IdentityTable.id,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE
    )
    val deviceId = reference(
        name = "device_id", refColumn = DeviceTable.id,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE
    )
    override val primaryKey: PrimaryKey
        get() = PrimaryKey(arrayOf(identityId, deviceId))
}