package org.burgas.router

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.burgas.dao.IdentityEntity
import org.burgas.database.DatabaseConnection
import org.burgas.database.IdentityTable
import org.burgas.dto.AuthData
import org.burgas.dto.AuthSession
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.mindrot.jbcrypt.BCrypt

fun Application.configureSecurityRouter() {

    routing {

        route("/api/v1/security") {

            post("/login") {
                val authSession = call.sessions.get(AuthSession::class)
                if (authSession != null) {
                    throw IllegalArgumentException("You already logged in")
                } else {
                    val authData = call.receive(AuthData::class)
                    val identity = suspendTransaction(db = DatabaseConnection.postgres, readOnly = true) {
                        IdentityEntity.find { IdentityTable.email eq authData.email }.singleOrNull()
                    }
                    if (identity != null && BCrypt.checkpw(authData.password, identity.password)) {
                        val authSession = AuthSession(identity.id.value, identity.authority)
                        call.sessions.set(authSession, AuthSession::class)
                        call.respond(HttpStatusCode.OK, "You successfully logged in")
                    } else {
                        throw IllegalArgumentException("Not authenticated in login. Wrong authentication data.")
                    }
                }
            }

            authenticate("auth-session-all") {

                post("/logout") {
                    call.sessions.clear(AuthSession::class)
                    call.respond(HttpStatusCode.OK, "You successfully logged out")
                }
            }
        }
    }
}