package starfield.model

import kotlinx.serialization.Serializable
import starfield.Id
import starfield.routing.Deck
import starfield.routing.DeckCard
import java.util.*

typealias Card = DeckCard

@Serializable
class BoardCard(val card: Card) {
    val id: Id = UUID.randomUUID()
    val visibility = mutableSetOf<Id>()
    var x = 0.0
    var y = 0.0
    var pivot = Pivot.UNTAPPED
    var counter = 0
    var transformed = false
    var zone = Zone.LIBRARY

    fun reset(clearVisibility: Boolean = true) {
        x = 0.0
        y = 0.0
        pivot = Pivot.UNTAPPED
        counter = 0
        if (clearVisibility) {
            visibility.clear()
        }
        transformed = false
        zone = Zone.LIBRARY
    }
}

sealed class BoardDiffEvent {
    data class ChangeZone(val card: UUID, val newZone: Zone, val oldZone: Zone) : BoardDiffEvent()
    data class ChangeIndex(val card: UUID, val newIndex: Int, val oldIndex: Int) : BoardDiffEvent()
    data class ChangePosition(val card: UUID, val x: Double, val y: Double) : BoardDiffEvent()

    data class ChangeAttribute(val card: UUID, val attribute: CardAttribute, val newValue: Int) : BoardDiffEvent()
    data class ChangePlayerAttribute(val attribute: PlayerAttribute, val newValue: Int) : BoardDiffEvent()

    object ShuffleDeck : BoardDiffEvent()
    object ScoopDeck : BoardDiffEvent()
}

enum class PlayerAttribute {
    LIFE, POISON
}

enum class Pivot {
    UNTAPPED,
    TAPPED,
    LEFT_TAPPED,
    UPSIDE_DOWN,
}

enum class CardAttribute {
    PIVOT,
    COUNTER,
    TRANSFORMED
}

inline fun <reified T : Enum<T>> Int.toEnum(): T? {
    return enumValues<T>().firstOrNull { it.ordinal == this }
}

class BoardManager(private val owner: UUID, private val game: Game, private val deck: Deck) {
    private val cards: Map<Zone, MutableList<BoardCard>> = Zone.entries.associateWith {
        if (it == Zone.LIBRARY) {
            deck.maindeck.map { card -> BoardCard(card) }.toMutableList()
        } else {
            mutableListOf()
        }
    }

    fun mulligan(startingHandSize: Int): List<BoardDiffEvent> {
        val cardMoves = cards[Zone.HAND]!!
            .map { BoardDiffEvent.ChangeZone(it.id, Zone.LIBRARY, Zone.HAND) }
            .toMutableList<BoardDiffEvent>()

        cards[Zone.LIBRARY]!!.addAll(cards[Zone.HAND]!!)
        cards[Zone.HAND]!!.clear()

        cardMoves.addAll(drawCards(startingHandSize))

        return cardMoves
    }

    fun drawCards(count: Int): List<BoardDiffEvent> {
        return (0..count).flatMap { drawCard() }
    }

    fun drawCard(): List<BoardDiffEvent> {
        if (cards[Zone.LIBRARY]!!.size > 0) {
            val card = cards[Zone.LIBRARY]!!.removeAt(0)

            cards[Zone.HAND]!!.add(card)
            return listOf(BoardDiffEvent.ChangeZone(card.id, Zone.HAND, Zone.LIBRARY),
                BoardDiffEvent.ChangeIndex(card.id, cards[Zone.HAND]!!.size - 1, 0))
        }
        return listOf()
    }

    fun shuffleDeck(): BoardDiffEvent {
        cards[Zone.LIBRARY]!!.shuffle()
        cards[Zone.LIBRARY]!!.forEach {
            it.visibility.clear()
        }

        return BoardDiffEvent.ShuffleDeck
    }

    fun setAttribute(card: UUID, attribute: CardAttribute, value: Int): BoardDiffEvent? {
        val targetCard = findCard(card) ?: return null

        when(attribute) {
            CardAttribute.PIVOT -> targetCard.pivot = value.toEnum<Pivot>()!!
            CardAttribute.COUNTER -> targetCard.counter = value
            CardAttribute.TRANSFORMED -> targetCard.transformed = value != 0
        }
        return BoardDiffEvent.ChangeAttribute(card, attribute, value, )
    }


    /**
     * move cases:
     *
     * 1. Move to battlefield (x, y)
     * 2. Move on battlefield (x, y)
     * 3. Move in other zone (index)
     * 4. Move to other zone (index, zone)
     */
    fun moveCard(cardId: Id, x: Double, y: Double): List<BoardDiffEvent> {
        val events = mutableListOf<BoardDiffEvent>()
        val card = findCard(cardId) ?: return events
        card.x = x
        card.y = y
        events.add(BoardDiffEvent.ChangePosition(cardId, x, y))
        return events
    }

    fun moveCard(cardId: Id, newIndex: Int): List<BoardDiffEvent> {
        val events = mutableListOf<BoardDiffEvent>()
        val card = findCard(cardId) ?: return events

        val oldIndex = cards[card.zone]!!.indexOf(card)
        cards[card.zone]!!.removeAt(oldIndex)
        cards[card.zone]!!.add(newIndex, card)

        events.add(BoardDiffEvent.ChangeIndex(cardId, newIndex, oldIndex))

        return events
    }


    fun reset(): BoardDiffEvent {
        for(entry in cards) {
            for(card in entry.value) {
                card.reset()
            }
            if (entry.key != Zone.LIBRARY) {
                cards[Zone.LIBRARY]!!.addAll(entry.value)
                entry.value.clear()
            }
        }

        cards[Zone.LIBRARY]!!.shuffle()

        return BoardDiffEvent.ScoopDeck
    }


    private fun findCard(id: UUID): BoardCard? {
        return cards.values.flatten().find { it.id == id }
    }

    fun getState(): Map<Zone, List<BoardCard>> {
        return cards
    }

    fun changeZone(cardId: Id, newZone: Zone): List<BoardDiffEvent> {
        val events = mutableListOf<BoardDiffEvent>()
        val card = findCard(cardId) ?: return events

        val oldZone = card.zone
        val oldIndex = cards[oldZone]!!.indexOf(card)
        cards[oldZone]!!.remove(card)
        cards[newZone]!!.add(card)

        card.reset(clearVisibility = false)
        if (newZone.isPublic) {
            // visible to everyone
            game.users().forEach {
                card.visibility.add(it.id)
            }
        } else if (newZone == Zone.HAND) {
            // visible to just the owner
            card.visibility.add(owner)
        }

        card.zone = newZone

        events.add(BoardDiffEvent.ChangeZone(cardId, newZone, oldZone))
        events.add(BoardDiffEvent.ChangeIndex(cardId, cards[newZone]!!.size - 1, oldIndex))

        return events
    }
}

@Serializable
data class PlayerState(
    val name: String,
    val id: Id,
    val board: Map<Zone, List<BoardCard>>,
    val life: Int,
    val poison: Int
)

class Player(val user: User, deck: Deck, game: Game) {

    private val board = BoardManager(user.id, game, deck)
    private var life = 20
    private var poison = 0

    fun mulligan(): List<BoardDiffEvent> {
        return board.mulligan(7)
    }

    fun tapCard(card: UUID): List<BoardDiffEvent> {
        val event = board.setAttribute(card, CardAttribute.PIVOT, Pivot.TAPPED.ordinal)
        return if (event != null) {
            listOf(event)
        } else {
            listOf()
        }
    }

    fun getState(): PlayerState {
        return PlayerState(
            user.name,
            user.id,
            board.getState(),
            life, poison
        )
    }

    fun drawCards(count: Int): List<BoardDiffEvent> {
        return board.drawCards(count)
    }

    fun playCard(card: Id, x: Double, y: Double): List<BoardDiffEvent> {
        return board.changeZone(card, Zone.BATTLEFIELD) + board.moveCard(card, x, y)
    }

    fun moveCard(card: Id, index: Int): List<BoardDiffEvent> {
        return board.moveCard(card, index)
    }

    fun moveCard(card: Id, zone: Zone, index: Int): List<BoardDiffEvent> {
        val events = board.changeZone(card, zone)
        return if (index != -1) {
            events + board.moveCard(card, index)
        } else {
            events
        }
    }

    fun scoop(): List<BoardDiffEvent> {
        return listOf(board.reset())
    }

    fun shuffle(): List<BoardDiffEvent> {
        return listOf(board.shuffleDeck())
    }

    fun setLife(newValue: Int): List<BoardDiffEvent> {
        life = newValue
        return listOf(BoardDiffEvent.ChangePlayerAttribute(PlayerAttribute.LIFE, newValue))
    }

    fun setPoison(newValue: Int): List<BoardDiffEvent> {
        poison = newValue
        return listOf(BoardDiffEvent.ChangePlayerAttribute(PlayerAttribute.POISON, newValue))
    }
}

enum class Zone {
    LIBRARY,
    HAND,
    BATTLEFIELD,
    GRAVEYARD,
    EXILE,
    FACE_DOWN;

    val isPublic: Boolean
        get() {
            return this == BATTLEFIELD || this == GRAVEYARD || this == EXILE
        }
}