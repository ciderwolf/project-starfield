package starfield.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import starfield.data.dao.UserDao
import starfield.routing.*
import java.util.*

fun Application.configureRouting() {
    routing {
        route("/api") {
            post("/login") {
                val params = call.receive<Map<String, String>>()
                val username = params["name"]
                val password = params["password"]
                if (username == null || password == null) {
                    return@post call.respondError("Username and password must be supplied")
                }
                val result = UserDao().login(username, password) ?: return@post call.respondError("Invalid username or password")

                initSession(username, result)
            }

            get("/logout") {
                call.sessions.clear<UserSession>()
                call.respondSuccess("Logged out")
            }

            post("/create-account") {
                val params = call.receive<Map<String, String>>()
                val username = params["name"]
                val password = params["password"]
                if (username == null || password == null) {
                    return@post call.respondError("Username and password must be supplied")
                }
                if (username == "" || password == "") {
                    return@post call.respondError("Username and password must be supplied")
                }

                val dao = UserDao()
                val uid = dao.registerUser(username, password) ?: return@post call.respondError("That username is already taken")
                initSession(username, uid)
            }

            get("/authenticate") {
                val currentSession = call.sessions.get<UserSession>()
                if (currentSession == null) {
                    call.respondSuccess(false)
                } else {
                    call.respondSuccess(true)
                }
            }

            route("/lobbies") {
                gameRouting()
            }

            route("/game") {
                engineRouting()
            }

            route("/deck") {
                deckRouting()
            }

            route("/card") {
                cardRouting()
            }

            route("/cube") {
                cubeRouting()
            }

            route("/draft") {
                draftRouting()
            }
        }

        singlePageApplication {
            vue("client-app")
        }
    }
}

suspend fun RoutingContext.initSession(username: String, userId: UUID) {
    val currentSession = call.sessions.get<UserSession>()
    if (currentSession == null) {
        val session = UserSession(username, userId)
        call.sessions.set(session)
        call.respondSuccess(session)
    } else {
        call.respondSuccess(currentSession)
    }
}

@Serializable
sealed class ResponseMessage(val success: Boolean)

@Serializable
data class ErrorMessage(
    val message: String,
    val code: Int
) : ResponseMessage(false)

@Serializable
data class SuccessMessage<T>(
    val content: T
) : ResponseMessage(true)

suspend fun ApplicationCall.respondValidationError(message: String) {
    this.respondError(message, HttpStatusCode.UnprocessableEntity)
}

suspend fun ApplicationCall.respondError(message: String, status: HttpStatusCode = HttpStatusCode.BadRequest) {
    this.respond(status, ErrorMessage(message, status.value))
}

suspend inline fun <reified T> ApplicationCall.respondSuccess(message: T, status: HttpStatusCode = HttpStatusCode.OK) {
    this.respond(status, SuccessMessage(message))
}