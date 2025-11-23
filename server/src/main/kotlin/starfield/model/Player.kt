package starfield.model

import kotlinx.serialization.Serializable
import starfield.plugins.Id
import starfield.engine.*
import starfield.routing.Deck
import java.util.*


@Serializable
data class PlayerState(
    val name: String,
    val id: Id,
    val board: Map<Zone, List<CardState>>,
    val cardToOracleId: Map<CardId, OracleId>,
    val life: Int,
    val poison: Int
)

class Player(val user: User, val userIndex: Int, deck: Deck, val game: Game) {

    private val board = BoardManager(user.id, userIndex, game, deck)
    private var life = 20
    private var poison = 0

    private fun resetSelf(): List<BoardDiffEvent> {
        val events = mutableListOf<BoardDiffEvent>()
        if (life != 20) {
            life = 20
            events.add(BoardDiffEvent.ChangePlayerAttribute(user.id, PlayerAttribute.LIFE, 20))
        }

        if (poison != 0) {
            poison = 0
            events.add(BoardDiffEvent.ChangePlayerAttribute(user.id, PlayerAttribute.POISON, 0))
        }
        return events
    }

    fun mulligan(): List<BoardDiffEvent> {
        return board.reset() + board.drawCards(7, to = Zone.HAND, fromBottom = false)
    }

    fun getState(playerId: UUID): PlayerState {
        val cardToOracle = board.getOracleInfo(playerId)
        return PlayerState(
            user.name,
            user.id,
            board.getState(),
            cardToOracle,
            life,
            poison
        )
    }

    fun drawCards(count: Int, to: Zone, fromBottom: Boolean): List<BoardDiffEvent> {
        return board.drawCards(count, to, fromBottom)
    }

    fun playCard(card: CardId, x: Double, y: Double, attributes: List<CardAttribute>): List<BoardDiffEvent> {
        val playFaceDown = attributes
            .filterIsInstance<CardAttribute.Flipped>()
            .any { it.flipped }
        val changeZoneResult = board.changeZone(card, Zone.BATTLEFIELD, playFaceDown)

        return if (changeZoneResult.cardWasMoved) {
            val updatedCardId = changeZoneResult.newCardId

            changeZoneResult.events +
                    board.moveCard(updatedCardId, x, y) +
                    attributes.flatMap { board.setAttribute(updatedCardId, it) }
        } else {
            changeZoneResult.events
        }
    }

    fun moveCard(card: CardId, x: Double, y: Double): List<BoardDiffEvent> {
        return board.moveCard(card, x, y)
    }

    fun moveCard(card: CardId, index: Int): List<BoardDiffEvent> {
        return board.moveCard(card, index)
    }

    fun moveCard(card: CardId, zone: Zone, index: Int): List<BoardDiffEvent> {
        val changeZoneResult = board.changeZone(card, zone)
        return if (index != -1 && changeZoneResult.cardWasMoved) {
            changeZoneResult.events + board.moveCard(changeZoneResult.newCardId, index)
        } else {
            changeZoneResult.events
        }
    }

    fun moveCards(cards: List<CardId>, zone: Zone, index: Int): List<BoardDiffEvent> {
        return cards.flatMap { moveCard(it, zone, index) }
    }

    fun scoop(): List<BoardDiffEvent> {
        return board.reset() + resetSelf()
    }

    fun shuffle(): List<BoardDiffEvent> {
        return listOf(board.shuffleDeck())
    }

    fun setLife(newValue: Int): List<BoardDiffEvent> {
        life = newValue
        return listOf(BoardDiffEvent.ChangePlayerAttribute(user.id, PlayerAttribute.LIFE, newValue))
    }

    fun setPoison(newValue: Int): List<BoardDiffEvent> {
        poison = newValue
        return listOf(BoardDiffEvent.ChangePlayerAttribute(user.id, PlayerAttribute.POISON, newValue))
    }

    fun getOracleId(card: CardId): OracleId {
        return board.getOracleId(card)
    }

    fun getClonedCard(card: CardId, toBeControlledByPlayerIndex: Int): BoardCard {
        val card = board.findCard(card)!!
        return card.clone(CardOrigin.TOKEN, toBeControlledByPlayerIndex)
    }

    fun changeAttribute(card: CardId, attribute: CardAttribute): List<BoardDiffEvent> {
        return board.setAttribute(card, attribute)
    }

    fun revealCard(card: CardId, revealTo: Id?, reveal: Boolean): List<BoardDiffEvent> {
        return if (reveal) {
            board.revealTo(card, revealTo)
        } else {
            board.unrevealTo(card, revealTo)
        }
    }

    fun scry(count: Int): List<BoardDiffEvent> {
        return board.scry(count)
    }

    fun getVirtualIds(): Map<Zone, Map<Id, OracleId>> {
        return board.getVirtualIds()
    }

    fun moveCardsVirtual(virtualIds: List<Id>, zone: Zone, index: Int): List<BoardDiffEvent> {
        return board.getCardsFromVirtualIds(virtualIds).flatMap { moveCard(it.id, zone, index) }
    }

    suspend fun createToken(id: OracleId): List<BoardDiffEvent> {
        val card = game.cardInfoProvider.tryRegisterToken(id)
        return board.createCard(card)
    }

    suspend fun createCard(id: OracleId, attributes: List<CardAttribute>): List<BoardDiffEvent> {
        val createFaceDown = attributes
            .filterIsInstance<CardAttribute.Flipped>()
            .any { it.flipped }
        val card = game.cardInfoProvider.tryRegisterCard(id)
        val events = board.createCard(card, createFaceDown)
        val newCardId = events.filterIsInstance<BoardDiffEvent.CreateCard>().first().state.id
        return events + attributes.flatMap { board.setAttribute(newCardId, it) }
    }

    fun createCard(card: BoardCard): List<BoardDiffEvent> {
        return board.addCard(card)
    }

    fun cloneCard(card: CardId): List<BoardDiffEvent> {
        return board.cloneCard(card)
    }

    fun untapAll(): List<BoardDiffEvent> {
        return board.untapAll()
    }

    fun sideboard(main: List<Id>, side: List<Id>): List<BoardDiffEvent> {
        return board.sideboard(main, side) + resetSelf()
    }

    fun registerSpectator(user: User) {
        board.revealCardsToSpectator(user)
    }

    fun hasCard(cardId: CardId): Boolean {
        return board.findCard(cardId) != null
    }
}

