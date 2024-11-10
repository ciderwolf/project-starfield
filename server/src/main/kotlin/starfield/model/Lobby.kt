package starfield.model

import kotlinx.serialization.Serializable
import starfield.plugins.Id
import starfield.StateMessage
import starfield.UserListing
import starfield.plugins.UserCollection
import starfield.routing.Deck
import java.util.*

@Serializable
data class LobbyState(
    val id: Id,
    val name: String,
    val users: List<UserListing>,
    val decks: List<Id?>,
)

data class LobbyUser(val user: User, var deck: Deck?)

class Lobby(val id: UUID, val owner: User, val name: String, val players: Int): UserCollection<LobbyState>() {

    private val entrants = mutableMapOf<UUID, LobbyUser>()

    private var ownerDeck: Deck? = null

    private val createdTime = System.currentTimeMillis()

    fun startGame(): Game? {
        if (players == 1 && ownerDeck != null) {
            return Game(name, id, mapOf(owner to ownerDeck!!, owner to ownerDeck!!))
        }
        if (ownerDeck != null && entrants.size == players - 1 && entrants.all { it.value.deck != null }) {
            return Game(name, id, mapOf(owner to ownerDeck!!, *entrants.map { it.value.user to it.value.deck!! }.toTypedArray()))
        }
        return null
    }

    suspend fun setDeck(player: User, deck: Deck) {
        if (player == owner) {
            ownerDeck = deck
        } else {
            val lobbyUser = entrants[player.id]
            if (lobbyUser != null) {
                lobbyUser.deck = deck
            }
        }
        broadcast(StateMessage(currentState(owner.id), "lobby"))
    }

    fun canJoin(): Boolean = entrants.size < players - 1
    suspend fun join(other: User): Boolean {
        if (canJoin()) {
            entrants[other.id] = LobbyUser(other, null)
            broadcast(StateMessage(currentState(owner.id), "lobby"))
            return true
        }

        return false
    }

    suspend fun leave(user: User): Boolean {
        val left = if (user != owner) {
            entrants.remove(user.id) != null
        } else user == owner

        broadcastToEach {
            StateMessage(currentState(it.id), "lobby")
        }
        return left
    }

    suspend fun remove(other: UUID): Boolean {
        if (other in entrants) {
            entrants.remove(other)
            broadcastToEach {
                StateMessage(currentState(it.id), "lobby")
            }
            return true
        }
        return false
    }

    fun hasPlayer(userId: UUID): Boolean {
        return userId in entrants
                || owner.id == userId
    }

    override fun users(): List<User> {
        return listOf(owner) + entrants.values.map { it.user }
    }

    override fun currentState(playerId: UUID): LobbyState {
        if (players == 1) {
            return LobbyState(
                id, name, userListings(), listOf(ownerDeck?.id)
            )
        }

        val decks = mutableListOf(ownerDeck?.id)
        decks.addAll(entrants.values.map { it.deck?.id })
        while (decks.size < players) {
            decks.add(null)
        }
        return LobbyState(
            id, name, userListings(), decks
        )
    }

    fun lastActionTime() = createdTime

}