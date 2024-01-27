package starfield.engine

import starfield.plugins.Id
import starfield.data.dao.CardDao
import starfield.model.*
import starfield.plugins.toEnum
import starfield.routing.Deck
import java.util.*


class BoardManager(private val owner: UUID, private val ownerIndex: Int, private val game: Game, private val deck: Deck) {
    private val cards: Map<Zone, MutableList<BoardCard>> = Zone.entries.associateWith {
        when (it) {
            Zone.LIBRARY -> {
                deck.maindeck.flatMap { card ->
                    val oracleCard = game.cardInfoProvider[card.id]!!
                    (0..<card.count).map { BoardCard(oracleCard, game.cardIdProvider, ownerIndex, CardOrigin.DECK) }
                }.shuffled().toMutableList()
            }
            Zone.SIDEBOARD -> {
                val cards = deck.sideboard.flatMap { card ->
                    val oracleCard = game.cardInfoProvider[card.id]!!
                    (0..<card.count).map { BoardCard(oracleCard, game.cardIdProvider, ownerIndex, CardOrigin.SIDEBOARD) }
                }.toMutableList()
                cards.forEach { card ->
                    card.zone = Zone.SIDEBOARD
                    card.visibility.add(owner)
                }
                cards
            }
            else -> mutableListOf()
        }
    }

    fun drawCards(count: Int, to: Zone): List<BoardDiffEvent> {
        return (0..<count).flatMap { drawCard(to) }
    }

    private fun drawCard(to: Zone): List<BoardDiffEvent> {
        if (cards[Zone.LIBRARY]!!.size > 0) {
            return changeZone(cards[Zone.LIBRARY]!!.last().id, to).events
        }
        return listOf()
    }

    fun setAttribute(card: CardId, attribute: CardAttribute, value: Int): List<BoardDiffEvent> {
        val targetCard = findCard(card) ?: return listOf()

        when(attribute) {
            CardAttribute.PIVOT -> targetCard.pivot = value.toEnum<Pivot>()!!
            CardAttribute.COUNTER -> targetCard.counter = value
            CardAttribute.TRANSFORMED -> targetCard.transformed = value != 0
            CardAttribute.FLIPPED -> targetCard.flipped = value != 0
        }
        val events = mutableListOf<BoardDiffEvent>(BoardDiffEvent.ChangeAttribute(card, attribute, value))
        if (attribute == CardAttribute.FLIPPED && value == 0 && targetCard.zone.isPublic) {
            events.addAll(revealToAll(targetCard))
        }

        return events
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
        val events = mutableListOf<BoardDiffEvent>()
        for(key in cards.keys) {
            val zoneCards = cards[key]!!
            
            // filter out tokens / created cards
            // TODO: should keep cards that were sideboarded in
            cards[key]!!.filter { it.origin != CardOrigin.DECK }.forEach {
                events.add(BoardDiffEvent.DestroyCard(it.id))
                zoneCards.remove(it)
            }

            if (key != Zone.LIBRARY && key != Zone.SIDEBOARD) {
                cards[Zone.LIBRARY]!!.addAll(zoneCards)
                zoneCards.clear()
            }
        }
        cards[Zone.LIBRARY]!!.forEach { it.reset(clearVisibility = true) }
        cards[Zone.LIBRARY]!!.shuffle()
        events.add(BoardDiffEvent.ScoopDeck(owner, cards[Zone.LIBRARY]!!.map { it.id }))
        return events
    }

    private fun findCard(id: CardId): BoardCard? {
        return cards.values.flatten().find { it.id == id }
    }

    fun getState(): Map<Zone, List<CardState>> {
        return cards.map { Pair(it.key, it.value.mapIndexed { index, card -> card.getState(index) }) }.toMap()
    }

    fun changeZone(cardId: CardId, newZone: Zone, playFaceDown: Boolean = false): MoveZoneResult {
        val events = mutableListOf<BoardDiffEvent>()
        val card = findCard(cardId) ?: return MoveZoneResult(events, cardId)

        if (card.origin == CardOrigin.TOKEN) {
            cards[card.zone]!!.remove(card)
            events.add(BoardDiffEvent.DestroyCard(cardId))
            return MoveZoneResult(events, cardId)
        }

        val oldZone = card.zone
        val oldCardId = card.id
        cards[oldZone]!!.remove(card)
        cards[newZone]!!.add(card)

        card.reset(clearVisibility = false)
        card.zone = newZone

        if (!playFaceDown) {
            if (newZone.isPublic) {
                // visible to everyone
                events.addAll(revealToAll(card))
            } else if (newZone == Zone.HAND) {
                // visible to just the owner
                if (card.visibility.add(owner)) {
                    events.add(BoardDiffEvent.RevealCard(card.id, listOf(owner)))
                }
            }
        }

        events.add(BoardDiffEvent.ChangeZone(card.id, newZone, oldCardId))
//        events.add(BoardDiffEvent.ChangeIndex(cardId, cards[newZone]!!.size - 1))

        return MoveZoneResult(events, card.id)
    }

    private fun revealToAll(card: BoardCard): List<BoardDiffEvent> {
        val justAdded = mutableListOf<Id>()
        game.users().forEach {
            if (card.visibility.add(it.id)) {
                justAdded.add(it.id)
            }
        }

        return if (justAdded.size > 0) {
            listOf(BoardDiffEvent.RevealCard(card.id, justAdded))
        } else {
            listOf()
        }
    }

    fun getOracleId(card: CardId): OracleId {
        return findCard(card)!!.card.id
    }

    fun getOracleInfo(playerId: UUID): Pair<Map<CardId, OracleId>, Map<OracleId, CardDao.OracleCard>> {
        val cardToOracle = cards.values.flatten()
            .filter { it.visibility.contains(playerId) }
            .associate { Pair(it.id, it.card.id) }


        val oracleInfo = cardToOracle.values.associate {
            val card = game.cardInfoProvider[it]!!.toOracleCard()
            Pair(card.id, card)
        }

        return Pair(cardToOracle, oracleInfo)
    }

    fun revealTo(cardId: CardId, revealTo: Id?): List<BoardDiffEvent> {
        val card = findCard(cardId) ?: return listOf()
        val events = mutableListOf<BoardDiffEvent>()
        if (revealTo == null) {
            events.addAll(revealToAll(card))
        }
        else {
            if (card.visibility.add(revealTo)) {
                events.add(BoardDiffEvent.RevealCard(card.id, listOf(revealTo)))
            }
        }

        return events
    }

    fun getVirtualIds(): Map<UUID, OracleId> {
        cards[Zone.LIBRARY]!!.forEach {
            it.virtualId = UUID.randomUUID()
        }

        return cards[Zone.LIBRARY]!!
            .map { Pair(it.virtualId!!, it.card.id) }
            .shuffled()
            .toMap()
    }

    fun scry(count: Int): List<BoardDiffEvent> {
        val library = cards[Zone.LIBRARY]!!
        return (1..count).flatMap { revealTo(library[library.size - it].id, owner) }
    }

    fun getCardsFromVirtualIds(virtualIds: List<Id>): List<CardId> {
        val cards = cards[Zone.LIBRARY]!!.associateBy { it.virtualId }
        return virtualIds.mapNotNull { cards[it]?.id }
    }

    fun createCard(oracleCard: CardDao.CardEntity): List<BoardDiffEvent> {
        game.cardInfoProvider.registerCard(oracleCard)
        val boardCard = BoardCard(oracleCard, game.cardIdProvider, ownerIndex, CardOrigin.TOKEN)
        boardCard.zone = Zone.BATTLEFIELD
        cards[Zone.BATTLEFIELD]!!.add(boardCard)
        return listOf(BoardDiffEvent.CreateCard(boardCard.getState(cards[Zone.BATTLEFIELD]!!.size - 1))) +
                revealToAll(boardCard)
    }

    fun cloneCard(id: CardId): List<BoardDiffEvent> {
        val card = findCard(id) ?: return listOf()
        val newCard = card.clone(CardOrigin.TOKEN)
        val index = cards[card.zone]!!.indexOfFirst { it.id == id }
        cards[card.zone]!!.add(index + 1, newCard)
        return listOf(
            BoardDiffEvent.CreateCard(newCard.getState(index + 1)),
            BoardDiffEvent.RevealCard(newCard.id, newCard.visibility.toList())
        )
    }

    data class MoveZoneResult(val events: List<BoardDiffEvent>, val newCardId: CardId) {

        val cardWasMoved: Boolean
            get() = events.any { it is BoardDiffEvent.ChangeZone }
    }

}