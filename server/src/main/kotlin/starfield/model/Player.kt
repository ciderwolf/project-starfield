package starfield.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import starfield.Id
import starfield.plugins.PivotSerializer
import starfield.routing.Deck
import starfield.routing.DeckCard
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

typealias Card = DeckCard
typealias CardId = Int
typealias OracleId = String

class CardIdProvider {
    private var index = AtomicInteger(1)

    fun getId(zone: Zone, playerIndex: Int): CardId {
        val idx = index.getAndIncrement() shl 8
        return idx or (zone.ordinal shl 4) or playerIndex
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

class BoardCard(val card: Card, private val idProvider: CardIdProvider, private val playerIndex: Int) {
    val visibility = mutableSetOf<Id>()
    var x = 0.0
    var y = 0.0
    var pivot = Pivot.UNTAPPED
    var counter = 0
    var transformed = false
    var id: CardId = idProvider.getId(Zone.LIBRARY, playerIndex)
    var zone = Zone.LIBRARY
        set(value) {
            id = idProvider.getId(value, playerIndex)
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
        id = idProvider.getId(Zone.LIBRARY, playerIndex)
        transformed = false
        zone = Zone.LIBRARY
    }

    fun getState(index: Int): CardState {
        return CardState(id, x, y, index, pivot, counter, transformed, zone)
    }
}

@Serializable
sealed class BoardDiffEvent {
    @Serializable
    @SerialName("change_zone")
    data class ChangeZone(val card: CardId, val newZone: Zone, val oldCardId: CardId) : BoardDiffEvent()
    @Serializable
    @SerialName("change_index")
    data class ChangeIndex(val card: CardId, val newIndex: Int) : BoardDiffEvent()
    @Serializable
    @SerialName("change_position")
    data class ChangePosition(val card: CardId, val x: Double, val y: Double) : BoardDiffEvent()

    @Serializable
    @SerialName("reveal_card")
    data class RevealCard(val card: CardId, val players: List<Id>) : BoardDiffEvent()
    @Serializable
    @SerialName("change_attribute")
    data class ChangeAttribute(val card: CardId, val attribute: CardAttribute, val newValue: Int) : BoardDiffEvent()
    @Serializable
    @SerialName("change_player_attribute")
    data class ChangePlayerAttribute(val attribute: PlayerAttribute, val newValue: Int) : BoardDiffEvent()

    @Serializable
    @SerialName("shuffle_deck")
    data class ShuffleDeck(val playerId: Id, val newIds: List<CardId>) : BoardDiffEvent()

    @Serializable
    @SerialName("scoop_deck")
    data class ScoopDeck(val playerId: Id, val newIds: List<CardId>) : BoardDiffEvent()
}

enum class PlayerAttribute {
    LIFE, POISON
}

@Serializable(with = PivotSerializer::class)
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

class BoardManager(private val owner: UUID, ownerIndex: Int, private val game: Game, private val deck: Deck) {
    private val cards: Map<Zone, MutableList<BoardCard>> = Zone.entries.associateWith {
        if (it == Zone.LIBRARY) {
            deck.maindeck.flatMap { card ->
                (0..<card.count).map { BoardCard(card, game.cardIdProvider, ownerIndex) }
            }.toMutableList()
        } else {
            mutableListOf()
        }
    }

    fun drawCards(count: Int): List<BoardDiffEvent> {
        return (0..<count).flatMap { drawCard() }
    }

    private fun drawCard(): List<BoardDiffEvent> {
        if (cards[Zone.LIBRARY]!!.size > 0) {
            return changeZone(cards[Zone.LIBRARY]!![0].id, Zone.HAND)
        }
        return listOf()
    }

    fun setAttribute(card: CardId, attribute: CardAttribute, value: Int): List<BoardDiffEvent> {
        val targetCard = findCard(card) ?: return listOf()

        when(attribute) {
            CardAttribute.PIVOT -> targetCard.pivot = value.toEnum<Pivot>()!!
            CardAttribute.COUNTER -> targetCard.counter = value
            CardAttribute.TRANSFORMED -> targetCard.transformed = value != 0
        }
        return listOf(BoardDiffEvent.ChangeAttribute(card, attribute, value))
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

        val trueIndex = if (newIndex == -1) { cards[card.zone]!!.size } else { newIndex }
        cards[card.zone]!!.add(trueIndex, card)

        events.add(BoardDiffEvent.ChangeIndex(cardId, trueIndex))

        return events
    }

    fun shuffleDeck(): BoardDiffEvent {
        cards[Zone.LIBRARY]!!.shuffle()
        cards[Zone.LIBRARY]!!.forEach {
            it.visibility.clear()
        }

        return BoardDiffEvent.ShuffleDeck(owner, cards[Zone.LIBRARY]!!.map { it.id })
    }

    fun reset(): List<BoardDiffEvent> {
        for(entry in cards) {
            if (entry.key != Zone.LIBRARY) {
                cards[Zone.LIBRARY]!!.addAll(entry.value)
                entry.value.clear()
            }
        }
        cards[Zone.LIBRARY]!!.forEach { it.reset(clearVisibility = true) }
        cards[Zone.LIBRARY]!!.shuffle()
        return listOf(BoardDiffEvent.ScoopDeck(owner, cards[Zone.LIBRARY]!!.map { it.id }))
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
        val oldCardId = card.id
        cards[oldZone]!!.remove(card)
        cards[newZone]!!.add(card)

        card.reset(clearVisibility = false)
        card.zone = newZone

        if (newZone.isPublic) {
            // visible to everyone
            val justAdded = mutableListOf<Id>()
            game.users().forEach {
                if (card.visibility.add(it.id)) {
                    justAdded.add(it.id)
                }
            }
            if (justAdded.size > 0) {
                events.add(BoardDiffEvent.RevealCard(card.id, justAdded))
            }
        } else if (newZone == Zone.HAND) {
            // visible to just the owner
            if (card.visibility.add(owner)) {
                events.add(BoardDiffEvent.RevealCard(card.id, listOf(owner)))
            }
        }

        events.add(BoardDiffEvent.ChangeZone(card.id, newZone, oldCardId))
//        events.add(BoardDiffEvent.ChangeIndex(cardId, cards[newZone]!!.size - 1))

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

class Player(val user: User, userIndex: Int, deck: Deck, game: Game) {

    private val board = BoardManager(user.id, userIndex, game, deck)
    private var life = 20
    private var poison = 0

    fun mulligan(): List<BoardDiffEvent> {
        return board.reset() + board.drawCards(7)
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
        val changeZoneResult = board.changeZone(card, Zone.BATTLEFIELD)
        return if (changeZoneResult.isNotEmpty() && changeZoneResult.last() is BoardDiffEvent.ChangeZone) {
            val updatedCardId = (changeZoneResult.last() as BoardDiffEvent.ChangeZone).card
            changeZoneResult + board.moveCard(updatedCardId, x, y)
        } else {
            changeZoneResult
        }
    }

    fun moveCard(card: CardId, x: Double, y: Double): List<BoardDiffEvent> {
        return board.moveCard(card, x, y)
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

    fun changeAttribute(card: CardId, attribute: CardAttribute, newValue: Int): List<BoardDiffEvent> {
        return board.setAttribute(card, attribute, newValue)
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