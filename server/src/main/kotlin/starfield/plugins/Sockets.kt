package starfield.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import starfield.*
import starfield.model.Draft
import starfield.model.Game
import kotlin.collections.LinkedHashSet
import kotlin.time.Duration
import java.util.*

val connections: MutableSet<WSConnection> = Collections.synchronizedSet(LinkedHashSet())

class WSConnection(val id: UUID, val ws: DefaultWebSocketSession) {

    private var lastMessageTime = System.currentTimeMillis()

    fun lastMessageTime() = lastMessageTime

    fun receiveMessage() {
        lastMessageTime = System.currentTimeMillis()
    }

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
    HOME, LOBBY, GAME, DRAFT, DECK_BUILDER, LOGIN
}

suspend fun reconnect(connection: WSConnection) {
    val lobby = findLobby(connection.id)
    lobby?.reconnectUser(connection)
    if (lobby != null) {
        connection.send(LocationMessage(Location.LOBBY, lobby.id))
        connection.send(StateMessage(lobby.currentState(connection.id), Location.LOBBY))
        return
    }

    val game = findGame(connection.id)
    game?.reconnectUser(connection)
    if (game != null) {
        val state = game.currentState(connection.id)
        connection.send(LocationMessage(game.location(), game.id))
        connection.send(StateMessage(state, game.location()))

        return
    }
}

suspend fun disconnect(connection: WSConnection) {
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
        pingPeriod = Duration.parse("15s")
        timeout = Duration.parse("15s")
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
                    connection.receiveMessage()
                    if (message is GameMessage && game is Game) {
                        game.handleMessage(session.id, message)
                    } else if (message is DraftMessage && game is Draft) {
                        game.handleMessage(session.id, message)
                    }
                }
            } finally {
                println("Connection ${connection.id} closed.")
                connections -= connection
                disconnect(connection)
            }
        }
    }
}
