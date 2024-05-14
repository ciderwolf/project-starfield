package starfield.engine

import starfield.plugins.Id
import starfield.data.dao.CardDao
import starfield.model.*
import starfield.plugins.toEnum
import starfield.routing.Deck
import java.util.*
import kotlin.math.min


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

    fun drawCards(count: Int, to: Zone, fromBottom: Boolean): List<BoardDiffEvent> {
        return (0..<count).flatMap { drawCard(to, fromBottom) }
    }

    private fun drawCard(to: Zone, fromBottom: Boolean): List<BoardDiffEvent> {
        if (cards[Zone.LIBRARY]!!.size > 0) {
            val targetCard = if (fromBottom) {
                cards[Zone.LIBRARY]!![0]
            } else {
                cards[Zone.LIBRARY]!!.last()
            }
            return changeZone(targetCard.id, to).events
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
            for(i in zoneCards.size - 1 downTo 0) {
                val card = zoneCards[i]
                when(card.origin) {
                    CardOrigin.DECK -> {
                        // move sideboarded cards back to deck
                        if (key == Zone.SIDEBOARD) {
                            events.addAll(changeZone(card.id, Zone.LIBRARY).events)
                        }
                    }
                    CardOrigin.SIDEBOARD -> {
                        // move cards played from sideboard back
                        if (card.zone != Zone.SIDEBOARD) {
                            events.addAll(changeZone(card.id, Zone.SIDEBOARD).events)
                        }
                    }
                    CardOrigin.TOKEN -> {
                        // destroy tokens or cards inserted from outside the game
                        events.add(BoardDiffEvent.DestroyCard(card.id))
                        zoneCards.remove(card)
                    }
                }
            }

            if (key != Zone.LIBRARY && key != Zone.SIDEBOARD) {
                cards[Zone.LIBRARY]!!.addAll(zoneCards)
                zoneCards.clear()
            }
        }

        // make sure all sideboard cards are visible
        events.addAll(cards[Zone.SIDEBOARD]!!.flatMap { revealTo(it.id, owner) })

        cards[Zone.LIBRARY]!!.forEach { it.reset(clearVisibility = true) }
        cards[Zone.LIBRARY]!!.shuffle()
        events.add(BoardDiffEvent.ScoopDeck(owner, cards[Zone.LIBRARY]!!.map { it.id }))
        return events
    }

    private fun findCard(id: CardId): BoardCard? {
        val zone = CardIdProvider.getZone(id)
        return cards[zone]!!.find { it.id == id }
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
                events.addAll(revealTo(card.id, owner))
            }
        } else {
            // visible to just the owner
            events.addAll(revealTo(card.id, owner))
        }

        events.add(BoardDiffEvent.ChangeZone(card.id, newZone, oldCardId))

        return MoveZoneResult(events, card.id)
    }

    private fun revealToAll(card: BoardCard): List<BoardDiffEvent> {
        val justAdded = mutableListOf<Id>()
        for (subscriber in game.subscribers()) {
            if (card.visibility.add(subscriber.id)) {
                justAdded.add(subscriber.id)
            }
        }

        return if (justAdded.size > 0) {
            listOf(BoardDiffEvent.RevealCard(card.id, justAdded))
        } else {
            listOf()
        }
    }

    fun revealTo(cardId: CardId, revealTo: Id?): List<BoardDiffEvent> {
        val card = findCard(cardId) ?: return listOf()
        val events = mutableListOf<BoardDiffEvent>()
        if (revealTo == null) {
            events.addAll(revealToAll(card))
        }
        else {
            if (card.visibility.add(revealTo)) {
                val reveals = tryRevealToSpectators(card)
                events.add(BoardDiffEvent.RevealCard(card.id, reveals + revealTo))
            }
        }

        return events
    }

    private fun visibleToSpectators(card: BoardCard): Boolean {
        return card.visibility.size > 0
    }

    private fun tryRevealToSpectators(card: BoardCard): List<Id> {
        return if (visibleToSpectators(card)) {
            val revealedToSpectators = mutableListOf<Id>()
            val spectators = game.subscribers().toSet() - game.users().toSet()
            for (spectator in spectators) {
                if (card.visibility.add(spectator.id)) {
                    revealedToSpectators.add(spectator.id)
                }
            }
            revealedToSpectators
        } else {
            listOf()
        }
    }

    fun unrevealTo(cardId: CardId, revealTo: Id?): List<BoardDiffEvent> {
        val card = findCard(cardId) ?: return listOf()
        val visibility = card.visibility.toMutableList()
        card.visibility.clear()
        if (revealTo != null) {
            visibility.remove(revealTo)
        } else {
            visibility.clear()
        }
        card.visibility.addAll(visibility)

        return listOf(
            BoardDiffEvent.HideCard(cardId, revealTo?.let { listOf(it) } ?: listOf()),
        )
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

    fun getVirtualIds(): Map<Zone, Map<Id, OracleId>> {
        cards.values.flatten().forEach {
            it.virtualId = UUID.randomUUID()
        }

        return cards.entries.associate { (zone, cards) ->
            val cardMap = cards.map { Pair(it.virtualId!!, it.card.id) }
                .shuffled()
                .toMap()
            Pair(zone, cardMap)
        }
    }

    fun scry(count: Int): List<BoardDiffEvent> {
        val library = cards[Zone.LIBRARY]!!
        val safeCount = min(count, library.size)
        return (1..safeCount).flatMap { revealTo(library[library.size - it].id, owner) }
    }

    fun getCardsFromVirtualIds(virtualIds: List<Id>): List<BoardCard> {
        val cards = cards.values.flatten().associateBy { it.virtualId }
        return virtualIds.mapNotNull { cards[it] }
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
        newCard.x += 0.02
        newCard.y += 0.02
        val index = cards[card.zone]!!.indexOfFirst { it.id == id }
        cards[card.zone]!!.add(index + 1, newCard)
        return listOf(
            BoardDiffEvent.CreateCard(newCard.getState(index + 1)),
            BoardDiffEvent.RevealCard(newCard.id, newCard.visibility.toList())
        )
    }

    fun untapAll(): List<BoardDiffEvent> {
        return cards[Zone.BATTLEFIELD]!!
            .filter { it.pivot != Pivot.UNTAPPED }
            .flatMap { setAttribute(it.id, CardAttribute.PIVOT,  Pivot.UNTAPPED.ordinal) }
    }

    fun sideboard(main: List<Id>, side: List<Id>): List<BoardDiffEvent> {
        val mainCards = getCardsFromVirtualIds(main)
        val sideCards = getCardsFromVirtualIds(side)
        mainCards.forEach {
            it.origin = CardOrigin.DECK
        }
        sideCards.forEach {
            it.origin = CardOrigin.SIDEBOARD
        }
        return reset()
    }

    fun revealCardsToSpectator(user: User): List<BoardDiffEvent> {
        val publicCards = cards.values
            .flatten()
            .filter(::visibleToSpectators)

        return publicCards.flatMap { revealTo(it.id, user.id) }
    }

    data class MoveZoneResult(val events: List<BoardDiffEvent>, val newCardId: CardId) {

        val cardWasMoved: Boolean
            get() = events.any { it is BoardDiffEvent.ChangeZone }
    }

}