package starfield.model

import starfield.StateMessage
import starfield.draft.SetInfo
import starfield.plugins.Location
import starfield.routing.Deck
import java.util.*


abstract class Lobby<out TState : ActiveCollectionState, TUser> : UserCollection<LobbyState>() {

    abstract fun startGame(): ActiveUserCollection<TState>?
    abstract fun canJoin(): Boolean
    abstract val owner: User
    protected val entrants = mutableMapOf<UUID, TUser>()

    private val createdTime = System.currentTimeMillis()
    override fun lastActionTime() = createdTime


    suspend fun leave(user: User): Boolean {
        val left = if (user != owner) {
            entrants.remove(user.id) != null
        } else user == owner

        broadcastToEach {
            StateMessage(currentState(it.id), Location.LOBBY)
        }
        return left
    }

    suspend fun remove(other: UUID): Boolean {
        if (other in entrants) {
            entrants.remove(other)
            broadcastToEach {
                StateMessage(currentState(it.id), Location.LOBBY)
            }
            return true
        }
        return false
    }

    override fun hasPlayer(userId: UUID): Boolean {
        return userId in entrants
                || owner.id == userId
    }

    abstract suspend fun join(user: User): Boolean
}

class GameLobby(override val id: UUID, override val owner: User, override val name: String, val players: Int): Lobby<GameState, GameLobby.LobbyUser>() {

    data class LobbyUser(val user: User, var deck: Deck?)

    private var ownerDeck: Deck? = null

    override fun startGame(): ActiveUserCollection<GameState>? {
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
        broadcast(StateMessage(currentState(owner.id), Location.LOBBY))
    }

    override fun canJoin(): Boolean = entrants.size < players - 1
    override suspend fun join(user: User): Boolean {
        if (canJoin()) {
            entrants[user.id] = LobbyUser(user, null)
            broadcast(StateMessage(currentState(owner.id), Location.LOBBY))
            return true
        }

        return false
    }

    override fun users(): List<User> {
        return listOf(owner) + entrants.values.map { it.user }
    }

    override fun currentState(playerId: UUID): LobbyState {
        if (players == 1) {
            return GameLobbyState(
                id, name, userListings(), listOf(ownerDeck?.id)
            )
        }

        val decks = mutableListOf(ownerDeck?.id)
        decks.addAll(entrants.values.map { it.deck?.id })
        while (decks.size < players) {
            decks.add(null)
        }
        return GameLobbyState(
            id, name, userListings(), decks
        )
    }
}

class DraftLobby(override val id: UUID, override val owner: User, override val name: String, val players: Int, val set: SetInfo, val bots: Int): Lobby<DraftState, User>() {
    override fun startGame(): ActiveUserCollection<DraftState>? {
        if (entrants.size + 1 + bots != players) {
            return null
        }
        val draft = Draft(id, name, set, entrants.values + owner, bots)
        return draft
    }

    override fun canJoin(): Boolean {
        return entrants.size < players - 1
    }

    override suspend fun join(user: User): Boolean {
        if (canJoin()) {
            entrants[user.id] = user
            broadcast(StateMessage(currentState(owner.id), Location.LOBBY))
            return true
        }
        return false
    }

    override fun users(): List<User> {
        return listOf(owner) + entrants.values
    }

    override fun currentState(playerId: UUID): LobbyState {
        return DraftLobbyState(
            id, name, userListings(), bots, set.code
        )
    }
}