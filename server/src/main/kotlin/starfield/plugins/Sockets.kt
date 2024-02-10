package starfield.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import starfield.*
import starfield.model.User
import kotlin.collections.LinkedHashSet
import java.time.Duration
import java.util.*

abstract class UserCollection<S> {
    abstract fun users(): List<User>
    abstract fun currentState(playerId: UUID): S

    open fun reconnectUser(connection: WSConnection) {
        val user = users().find { it.id == connection.id }
        if (user != null) {
            user.connection = connection
        }
    }

    open fun disconnectUser(connection: WSConnection) {
        val user = users().find { it.id == connection.id }
        if (user != null) {
            user.connection = null
        }
    }

    fun userListings(): List<UserListing> {
        return users().map { UserListing(it.id, it.name) }
    }

    protected suspend inline fun <reified T : ServerMessage> broadcast(message: T) {
        users().forEach {
            it.connection?.send(message)
        }
    }

    protected suspend inline fun <reified T : ServerMessage> broadcastToEach(messageFactory: (User) -> T) {
        users().forEach {
            it.connection?.send(messageFactory(it))
        }
    }

}


val connections: MutableSet<WSConnection> = Collections.synchronizedSet(LinkedHashSet())

class WSConnection(val id: UUID, val ws: DefaultWebSocketSession) {

    suspend inline fun <reified T> send(message: T) {
        ws.send(Json.encodeToString(message))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WSConnection

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}


enum class Location {
    HOME, LOBBY, GAME, DECK_BUILDER, LOGIN
}

suspend fun reconnect(connection: WSConnection) {
    val lobby = findLobby(connection.id)
    lobby?.reconnectUser(connection)
    if (lobby != null) {
        connection.send(LocationMessage(Location.LOBBY, lobby.id))
        connection.send(StateMessage(lobby.currentState(connection.id), "lobby"))
        return
    }

    val game = findGame(connection.id)
    game?.reconnectUser(connection)
    if (game != null) {
        connection.send(LocationMessage(Location.GAME, game.id))
        connection.send(StateMessage(game.currentState(connection.id), "game"))
        return
    }
}

fun disconnect(connection: WSConnection) {
    val lobby = findLobby(connection.id)
    lobby?.disconnectUser(connection)
    if (lobby != null) return

    val game = findGame(connection.id)
    game?.disconnectUser(connection)

    val spectatingGame = findGameSpectating(connection.id)
    spectatingGame?.removeSpectator(connection.id)
}


fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/ws") { // websocketSession
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                send(Json.encodeToString(LocationMessage(Location.LOGIN, null)))
                close(CloseReason(CloseReason.Codes.NORMAL, "Not authenticated"))
                return@webSocket
            }
            val connection = WSConnection(session.id, this)
            connections += connection
            connection.send(IdentityMessage(session.username, session.id))
            reconnect(connection)
            try {
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue

                    val message = Json.decodeFromString<ClientMessage>(frame.readText())
                    val game = findGame(session.id)
                    game?.handleMessage(session.id, message)
                }
            } finally {
                connections -= connection
                disconnect(connection)
            }
        }
    }
}
