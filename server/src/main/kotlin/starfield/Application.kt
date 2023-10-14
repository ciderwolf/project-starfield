package starfield

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import starfield.plugins.*
import java.util.UUID

typealias Id = @Serializable(with=UUIDSerializer::class) UUID

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureSessions()
    configureSockets()
    configureRouting()
    install(IgnoreTrailingSlash)
}
