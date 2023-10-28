package starfield.model

import kotlinx.serialization.Serializable
import starfield.*
import starfield.plugins.UserCollection
import starfield.routing.Deck
import java.util.*

@Serializable
data class GameState(val name: String, val players: List<PlayerState>)

class Game(val name: String, val id: UUID, players: Map<User, Deck>) : UserCollection<GameState>() {

    private val players: List<Player>
    public val cardIndexProvider = CardIndexProvider()

    init {
        this.players = players.map {
            Player(it.key, it.value, this)
        }
    }

    fun hasPlayer(userId: UUID): Boolean {
        return players.any { it.user.id == userId }
    }

    override fun users(): List<User> {
        return players.map { it.user }
    }

    override fun currentState(playerId: UUID): GameState {
        return GameState(name, players.map { it.getState(playerId) })
    }

    suspend fun handleMessage(userId: UUID, message: ClientMessage) {
        val player = players.find { it.user.id == userId } ?: return
        val messages = when(message) {
            is ChangeCardAttributeMessage -> player.tapCard(message.card)
            is ChangeCardPositionMessage -> player.playCard(message.card, message.x, message.y)
            is PlayCardMessage -> player.playCard(message.card, message.x, message.y)
            is ChangeCardIndexMessage -> player.moveCard(message.card, message.index)
            is ChangeCardZoneMessage -> player.moveCard(message.card, message.zone, message.index)
            is DrawCardMessage -> player.drawCards(message.count)
            is SpecialActionMessage -> when(message.action) {
                SpecialAction.MULLIGAN -> player.mulligan()
                SpecialAction.SCOOP -> player.scoop()
                SpecialAction.SHUFFLE -> player.shuffle()
            }
            is ChangePlayerAttributeMessage -> when (message.attribute) {
                PlayerAttribute.LIFE -> player.setLife(message.newValue)
                PlayerAttribute.POISON -> player.setPoison(message.newValue)
            }
        }

        broadcast(BoardUpdateMessage(messages))

        val playerReveals = mutableMapOf<Id, MutableMap<CardId, OracleId>>()
        for(msg in messages) {
            if (msg is BoardDiffEvent.RevealCard) {
                for(p in msg.players) {
                    if (!playerReveals.containsKey(p)) {
                        playerReveals[p] = mutableMapOf()
                    }
                    val oracleId = player.getOracleCard(msg.card)
                    playerReveals[p]!![msg.card] = oracleId
                }
            }
        }

        for(user in users()) {
            if (playerReveals.containsKey(user.id)) {
                user.connection?.send(OracleCardInfoMessage(playerReveals[user.id]!!))
            }
        }
    }
}

