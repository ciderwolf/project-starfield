package starfield.model

import kotlinx.serialization.Serializable
import starfield.Id
import starfield.routing.Deck
import starfield.routing.DeckCard
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

typealias Card = DeckCard
typealias CardId = Int
typealias OracleId = String

class CardIndexProvider {
    var index = AtomicInteger(1 shl 5)

    fun getIndex(): CardId {
        return getIndex(Zone.LIBRARY)
    }

    fun getIndex(zone: Zone): CardId {
        val idx = index.getAndIncrement() shl 4
        return idx or zone.ordinal
    }
}

@Serializable
data class CardState(
    val id: CardId,
    val x: Double,
    val y: Double,
    val index: Int,
    val pivot: Pivot,
    val counter: Int,
    val transformed: Boolean,
    val zone: Zone
)

class BoardCard(val card: Card, private val idProvider: CardIndexProvider) {
    val visibility = mutableSetOf<Id>()
    var x = 0.0
    var y = 0.0
    var pivot = Pivot.UNTAPPED
    var counter = 0
    var transformed = false
    var id: CardId = idProvider.getIndex()
    var zone = Zone.LIBRARY
        set(value) {
            idProvider.getIndex(value)
            field = value
        }

    fun reset(clearVisibility: Boolean = true) {
        x = 0.0
        y = 0.0
        pivot = Pivot.UNTAPPED
        counter = 0
        if (clearVisibility) {
            visibility.clear()
        }
        id = idProvider.getIndex(Zone.LIBRARY)
        transformed = false
        zone = Zone.LIBRARY
    }

    fun getState(index: Int): CardState {
        return CardState(id, x, y, index, pivot, counter, transformed, zone)
    }
}

@Serializable
sealed class BoardDiffEvent(val type: String) {
    data class ChangeZone(val card: CardId, val newZone: Zone, val oldCardId: CardId) : BoardDiffEvent("change_zone")
    data class ChangeIndex(val card: CardId, val newIndex: Int) : BoardDiffEvent("change_index")
    data class ChangePosition(val card: CardId, val x: Double, val y: Double) : BoardDiffEvent("change_position")

    data class RevealCard(val card: CardId, val players: List<Id>) : BoardDiffEvent("reveal_card")
    data class ChangeAttribute(val card: CardId, val attribute: CardAttribute, val newValue: Int) : BoardDiffEvent("change_attribute")
    data class ChangePlayerAttribute(val attribute: PlayerAttribute, val newValue: Int) : BoardDiffEvent("change_player_attribute")

    data class ShuffleDeck(val newIds: List<CardId>) : BoardDiffEvent("shuffle_deck")
    data class ScoopDeck(val newIds: List<CardId>) : BoardDiffEvent("scoop_deck")
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
            deck.maindeck.flatMap { card ->
                (0..card.count).map { BoardCard(card, game.cardIndexProvider) }
            }.toMutableList()
        } else {
            mutableListOf()
        }
    }

    fun drawCards(count: Int): List<BoardDiffEvent> {
        return (0..count).flatMap { drawCard() }
    }

    private fun drawCard(): List<BoardDiffEvent> {
        if (cards[Zone.LIBRARY]!!.size > 0) {
            val card = cards[Zone.LIBRARY]!!.removeAt(0)

            cards[Zone.HAND]!!.add(card)
            val oldCardId = card.id
            card.zone = Zone.HAND
            return listOf(BoardDiffEvent.ChangeZone(card.id, Zone.HAND, oldCardId),
                BoardDiffEvent.ChangeIndex(card.id, cards[Zone.HAND]!!.size - 1))
        }
        return listOf()
    }

    fun setAttribute(card: CardId, attribute: CardAttribute, value: Int): BoardDiffEvent? {
        val targetCard = findCard(card) ?: return null

        when(attribute) {
            CardAttribute.PIVOT -> targetCard.pivot = value.toEnum<Pivot>()!!
            CardAttribute.COUNTER -> targetCard.counter = value
            CardAttribute.TRANSFORMED -> targetCard.transformed = value != 0
        }
        return BoardDiffEvent.ChangeAttribute(card, attribute, value)
    }

    fun moveCard(cardId: CardId, x: Double, y: Double): List<BoardDiffEvent> {
        val events = mutableListOf<BoardDiffEvent>()
        val card = findCard(cardId) ?: return events
        card.x = x
        card.y = y
        events.add(BoardDiffEvent.ChangePosition(cardId, x, y))
        return events
    }

    fun moveCard(cardId: CardId, newIndex: Int): List<BoardDiffEvent> {
        val events = mutableListOf<BoardDiffEvent>()
        val card = findCard(cardId) ?: return events

        val oldIndex = cards[card.zone]!!.indexOf(card)
        cards[card.zone]!!.removeAt(oldIndex)
        cards[card.zone]!!.add(newIndex, card)

        events.add(BoardDiffEvent.ChangeIndex(cardId, newIndex))

        return events
    }

    fun shuffleDeck(): BoardDiffEvent {
        cards[Zone.LIBRARY]!!.shuffle()
        cards[Zone.LIBRARY]!!.forEach {
            it.visibility.clear()
        }

        return BoardDiffEvent.ShuffleDeck(cards[Zone.LIBRARY]!!.map { it.id })
    }

    fun reset(): List<BoardDiffEvent> {
        for(entry in cards) {
            if (entry.key != Zone.LIBRARY) {
                cards[Zone.LIBRARY]!!.addAll(entry.value)
                entry.value.clear()
                entry.value.forEach { it.reset(clearVisibility = true) }
            }
        }

        return listOf(BoardDiffEvent.ScoopDeck(cards[Zone.LIBRARY]!!.map { it.id }))
    }


    private fun findCard(id: CardId): BoardCard? {
        return cards.values.flatten().find { it.id == id }
    }

    fun getState(): Map<Zone, List<CardState>> {
        return cards.map { Pair(it.key, it.value.mapIndexed { index, card -> card.getState(index) }) }.toMap()
    }

    fun changeZone(cardId: CardId, newZone: Zone): List<BoardDiffEvent> {
        val events = mutableListOf<BoardDiffEvent>()
        val card = findCard(cardId) ?: return events

        val oldZone = card.zone
        cards[oldZone]!!.remove(card)
        cards[newZone]!!.add(card)

        card.reset(clearVisibility = false)
        if (newZone.isPublic) {
            // visible to everyone
            game.users().forEach {
                card.visibility.add(it.id)
            }
            events.add(BoardDiffEvent.RevealCard(cardId, game.users().map { it.id }))
        } else if (newZone == Zone.HAND) {
            // visible to just the owner
            card.visibility.add(owner)
            events.add(BoardDiffEvent.RevealCard(cardId, listOf(owner)))
        }

        val oldCardId = card.id
        card.zone = newZone

        events.add(BoardDiffEvent.ChangeZone(cardId, newZone, oldCardId))
        events.add(BoardDiffEvent.ChangeIndex(cardId, cards[newZone]!!.size - 1))

        return events
    }

    fun getOracleId(card: CardId): OracleId {
        return findCard(card)!!.card.image
    }

    fun getOracleInfo(playerId: UUID): Map<CardId, OracleId> {
        return cards.values.flatten()
            .filter { it.visibility.contains(playerId) }
            .associate { Pair(it.id, it.card.image) }
    }
}

@Serializable
data class PlayerState(
    val name: String,
    val id: Id,
    val board: Map<Zone, List<CardState>>,
    val oracleInfo: Map<CardId, OracleId>,
    val life: Int,
    val poison: Int
)

class Player(val user: User, deck: Deck, game: Game) {

    private val board = BoardManager(user.id, game, deck)
    private var life = 20
    private var poison = 0

    fun mulligan(): List<BoardDiffEvent> {
        return board.reset() + board.drawCards(7)
    }

    fun tapCard(card: CardId): List<BoardDiffEvent> {
        val event = board.setAttribute(card, CardAttribute.PIVOT, Pivot.TAPPED.ordinal)
        return if (event != null) {
            listOf(event)
        } else {
            listOf()
        }
    }

    fun getState(playerId: UUID): PlayerState {
        return PlayerState(
            user.name,
            user.id,
            board.getState(),
            board.getOracleInfo(playerId),
            life, poison
        )
    }

    fun drawCards(count: Int): List<BoardDiffEvent> {
        return board.drawCards(count)
    }

    fun playCard(card: CardId, x: Double, y: Double): List<BoardDiffEvent> {
        return board.changeZone(card, Zone.BATTLEFIELD) + board.moveCard(card, x, y)
    }

    fun moveCard(card: CardId, index: Int): List<BoardDiffEvent> {
        return board.moveCard(card, index)
    }

    fun moveCard(card: CardId, zone: Zone, index: Int): List<BoardDiffEvent> {
        val events = board.changeZone(card, zone)
        return if (index != -1) {
            events + board.moveCard(card, index)
        } else {
            events
        }
    }

    fun scoop(): List<BoardDiffEvent> {
        return board.reset()
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

    fun getOracleCard(card: CardId): OracleId {
        return board.getOracleId(card)
    }
}

enum class Zone {
    LIBRARY,
    HAND,
    BATTLEFIELD,
    GRAVEYARD,
    EXILE,
    FACE_DOWN,
    SIDEBOARD;

    val isPublic: Boolean
        get() {
            return this == BATTLEFIELD || this == GRAVEYARD || this == EXILE
        }
}