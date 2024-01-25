package starfield

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import starfield.data.dao.DatabaseSingleton
import starfield.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseSingleton.init()
    configureSerialization()
    configureSessions()
    configureSockets()
    configureRouting()
    install(IgnoreTrailingSlash)
}
