package org.burgas.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.config.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
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

    fun runMigration(dataSource: HikariDataSource) {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .locations("classpath:db/migration")
            .load()
        flyway.migrate()
    }

    val postgres = Database.connect(
        datasource = dataSource, databaseConfig = DatabaseConfig { this.explicitDialect = PostgreSQLDialect() }
    )

    val redis = Jedis(
        config.property("redis.host").getString(),
        config.property("redis.port").getString().toInt()
    )
}

fun configureDatabase() {
    DatabaseConnection.runMigration(DatabaseConnection.dataSource)
}