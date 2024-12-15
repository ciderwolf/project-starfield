package starfield.model

import starfield.ServerMessage
import starfield.UserListing
import starfield.plugins.WSConnection
import java.util.*

abstract class UserCollection<out S> {
    abstract fun users(): List<User>
    abstract fun currentState(playerId: UUID): S
    abstract fun lastActionTime(): Long
    abstract val id: UUID
    abstract val name: String
    abstract fun hasPlayer(userId: UUID): Boolean

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