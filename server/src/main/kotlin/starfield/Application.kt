package starfield

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import starfield.cli.Cli
import starfield.data.dao.DatabaseSingleton
import starfield.plugins.*

fun main(args: Array<String>) {
    if ("--cli" in args) {
        Cli.invoke(args.filter { it != "--cli" }.toTypedArray())
    } else {
        embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
    }
}

fun Application.module() {
    DatabaseSingleton.init()
    EntityPurge.init()
    configureSerialization()
    configureSessions()
    configureSockets()
    configureRouting()
    install(IgnoreTrailingSlash)

    // allow https headers to be set on https
    // (this is a hack)
    intercept(ApplicationCallPipeline.Plugins) {
        @Suppress( "DEPRECATION")
        val endpoint = call.attributes.computeIfAbsent(MutableOriginConnectionPointKey) {
            MutableOriginConnectionPoint(call.request.origin)
        }
        endpoint.scheme = "https"
    }
}
