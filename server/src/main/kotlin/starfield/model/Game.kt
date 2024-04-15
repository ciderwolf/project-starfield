package starfield.model

import kotlinx.serialization.Serializable
import starfield.*
import starfield.data.dao.CardDao
import starfield.engine.*
import starfield.plugins.Id
import starfield.plugins.UserCollection
import starfield.routing.Deck
import java.util.*

@Serializable
data class GameState(val id: Id, val name: String, val players: List<PlayerState>)

enum class AccountableAction {
    FIND_CARD, SCRY, SIDEBOARD, REVEAL
}

class Game(val name: String, val id: UUID, players: Map<User, Deck>) : UserCollection<GameState>() {

    private val players: List<Player>
    private val spectators: MutableList<User> = mutableListOf()
    private var lastActionTime = System.currentTimeMillis()
    val cardIdProvider = CardIdProvider()
    val cardInfoProvider = CardInfoProvider(players.values.flatMap { it.maindeck + it.sideboard })

    init {
        this.players = players.entries.mapIndexed { index, it ->
            Player(it.key, index, it.value, this)
        }
    }

    fun lastActionTime() = lastActionTime

    fun hasPlayer(userId: UUID): Boolean {
        return players.any { it.user.id == userId }
    }

    fun subscribers(): List<User> {
        return users() + spectators
    }

    override fun users(): List<User> {
        return players.map { it.user }
    }

    override fun currentState(playerId: UUID): GameState {
        return GameState(id, name, players.map { it.getState(playerId) })
    }

    suspend fun handleMessage(userId: UUID, message: ClientMessage) {
        val player = players.find { it.user.id == userId } ?: return
        lastActionTime = System.currentTimeMillis()
        println("Received message: $message")
        val messages = when(message) {
            is ChangeCardAttributeMessage -> player.changeAttribute(message.card, message.attribute, message.newValue)
            is ChangeCardPositionMessage -> player.moveCard(message.card, message.x, message.y)
            is PlayCardMessage -> player.playCard(message.card, message.x, message.y, message.attributes)
            is ChangeCardIndexMessage -> player.moveCard(message.card, message.index)
            is ChangeCardZoneMessage -> player.moveCard(message.card, message.zone, message.index)
            is ChangeCardZonesMessage -> player.moveCards(message.cards, message.zone, message.index)
            is MoveCardVirtualMessage -> player.moveCardsVirtual(message.ids, message.zone, message.index)
            is DrawCardMessage -> player.drawCards(message.count, message.to, message.fromBottom)
            is RevealCardMessage -> player.revealCard(message.card, message.revealTo, message.reveal)
            is ScryMessage -> player.scry(message.count)
            is CreateTokenMessage -> player.createToken(message.id)
            is CreateCardMessage -> player.createCard(message.id)
            is CloneCardMessage -> player.cloneCard(message.id)
            is SideboardMessage -> player.sideboard(message.main, message.side)
            is SpecialActionMessage -> when(message.action) {
                SpecialAction.MULLIGAN -> player.mulligan()
                SpecialAction.SCOOP -> player.scoop()
                SpecialAction.SHUFFLE -> player.shuffle()
                SpecialAction.UNTAP_ALL -> player.untapAll()
            }
            is ChangePlayerAttributeMessage -> when (message.attribute) {
                PlayerAttribute.LIFE -> player.setLife(message.newValue)
                PlayerAttribute.POISON -> player.setPoison(message.newValue)
            }
        }

        broadcastBoardUpdate(messages)

        val hiddenCardIds = messages.filterIsInstance<BoardDiffEvent.HideCard>()

        val revealedCardIds = messages.filterIsInstance<BoardDiffEvent.RevealCard>()
            .associate { Pair(it.card, player.getOracleCard(it.card)) }
        val revealedCards = revealedCardIds.values
            .associateWith { cardInfoProvider[it]!!.toOracleCard() }

        subscribers().forEach { user ->
            val playerReveals = mutableMapOf<CardId, OracleId>()
            val oracleCards = mutableMapOf<OracleId, CardDao.OracleCard>()
            messages.filterIsInstance<BoardDiffEvent.RevealCard>()
                .filter { it.players.contains(user.id) }
                .forEach { msg ->
                    val card = revealedCardIds[msg.card]!!
                    playerReveals[msg.card] = card
                    oracleCards[card] = revealedCards[card]!!
                }

            val cardHides = hiddenCardIds
                .filter { it.players.isEmpty() || it.players.contains(user.id) }
                .map { it.card }
            if (playerReveals.isNotEmpty() || cardHides.isNotEmpty()) {
                user.connection?.send(OracleCardInfoMessage(playerReveals, oracleCards, cardHides))
            }
        }

        if (message is ScryMessage) {
            sendAccountabilityMessage(AccountableAction.SCRY, player.user.id,  message.count)
        } else if (message is RevealCardMessage && message.reveal) {
            sendAccountabilityMessage(AccountableAction.REVEAL,  player.user.id, message.card, message.revealTo)
        }
    }

    fun end(user: User): Boolean {
        if (users().contains(user)) {
            return true
        }
        return false
    }

    suspend fun assignVirtualIds(userId: UUID, scoop: Boolean = false): Map<Zone, Map<UUID, OracleId>> {
        val player = players.find { it.user.id == userId } ?: return mapOf()
        if(scoop) {
            handleMessage(userId, SpecialActionMessage(SpecialAction.SCOOP))
        }
        return player.getVirtualIds()
    }

    suspend fun addSpectator(user: User) {
        if (spectators.any { it.id == user.id }) {
            return
        }
        spectators.add(user)
        players.forEach { it.registerSpectator(user) }
        user.connection?.send(StateMessage(currentState(user.id), "game"))
    }

    fun removeSpectator(userId: UUID) {
        spectators.removeIf { it.id == userId }
    }

    fun hasSpectator(userId: UUID): Boolean {
        return spectators.any { it.id == userId }
    }

    private suspend fun broadcastBoardUpdate(messages: List<BoardDiffEvent>) {
        val all = subscribers()
        for(user in all) {
            user.connection?.send(BoardUpdateMessage(messages))
        }
    }

    suspend fun sendAccountabilityMessage(messageType: AccountableAction, owner: Id, payload: Int = 0, target: Id? = null) {
        val all = subscribers()
        for(user in all) {
            user.connection?.send(AccountabilityMessage(messageType, owner, payload, target))
        }
    }
}

