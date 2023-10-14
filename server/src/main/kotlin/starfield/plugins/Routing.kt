package starfield.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.routing.deckRouting
import starfield.routing.gameRouting

fun Application.configureRouting() {
    routing {

        post("/login") {
            val name = call.receive<Map<String, String>>()["name"] ?: return@post call.respondError(
                "A name parameter must be specified")
            val currentSession = call.sessions.get<UserSession>()
            if (currentSession == null) {
                val session = UserSession.createNew(name)
                call.sessions.set(session)
                call.respondSuccess(session)
            } else {
                call.respondSuccess(currentSession)
            }
        }

        get("/") {
            call.respondText("Hello World!")
        }

        route("/lobbies") {
            gameRouting()
        }

        route("/deck") {
            deckRouting()
        }

        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }
}

@Serializable
sealed class ResponseMessage(val success: Boolean)

@Serializable
data class ErrorMessage(
    val message: String,
) : ResponseMessage(false)

@Serializable
data class SuccessMessage<T>(
    val content: T
) : ResponseMessage(true)

suspend fun ApplicationCall.respondError(message: String, status: HttpStatusCode = HttpStatusCode.BadRequest) {
    this.respond(status, ErrorMessage(message))
}

suspend inline fun <reified T> ApplicationCall.respondSuccess(message: T, status: HttpStatusCode = HttpStatusCode.OK) {
    this.respond(status, SuccessMessage(message))
}