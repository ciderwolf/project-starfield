package starfield.model

import kotlinx.serialization.Serializable
import starfield.Id
import starfield.StateMessage
import starfield.plugins.UserCollection
import starfield.routing.Deck
import java.util.*

@Serializable
data class LobbyState(
    val id: Id,
    val name: String,
    val users: List<Id>,
    val decks: List<Id?>,
)

class Lobby(val id: UUID, val owner: User, val name: String, val players: Int): UserCollection<LobbyState>() {

    private var otherPlayer: User? = null
    private var ownerDeck: Deck? = null
    private var otherDeck: Deck? = null

    fun startGame(): Game? {
        if (players == 1 && ownerDeck != null) {
            return Game(name, id, mapOf(owner to ownerDeck!!, owner to ownerDeck!!))
        }
        if (otherPlayer != null && ownerDeck != null && otherDeck != null) {
            return Game(name, id, mapOf(owner to ownerDeck!!, otherPlayer!! to otherDeck!!))
        }
        return null
    }

    suspend fun setDeck(player: User, deck: Deck) {
        if (player == owner) {
            ownerDeck = deck
        } else if (player == otherPlayer) {
            otherDeck = deck
        }
        broadcast(StateMessage(currentState(owner.id), "lobby"))
    }

    fun canJoin(): Boolean = otherPlayer == null
    suspend fun join(other: User): Boolean {
        if (canJoin()) {
            otherPlayer = other
            owner.connection?.send(StateMessage(currentState(owner.id), "lobby"))
            return true
        }

        return false
    }

    suspend fun leave(user: User): Boolean {
        val left = if (user == otherPlayer) {
            otherPlayer = null
            otherDeck = null
            true
        } else user == owner

        broadcastToEach {
            StateMessage(currentState(it.id), "lobby")
        }
        return left
    }

    suspend fun remove(other: UUID): Boolean {
        if (otherPlayer?.id == other) {
            otherPlayer = null
            otherDeck = null
            broadcastToEach {
                StateMessage(currentState(it.id), "lobby")
            }
            return true
        }
        return false

    }

    fun hasPlayer(userId: UUID): Boolean {
        return otherPlayer?.id == userId
                || owner.id == userId
    }

    override fun users(): List<User> {
        return if (otherPlayer == null) {
            listOf(owner)
        } else {
            listOf(owner, otherPlayer!!)
        }
    }

    override suspend fun currentState(playerId: UUID): LobbyState {
        if (players == 1) {
            return LobbyState(
                id, name, users().map { it.id }, listOf(ownerDeck).map { it?.id }
            )
        }
        return LobbyState(
            id, name, users().map { it.id }, listOf(ownerDeck, otherDeck).map { it?.id }
        )
    }
}