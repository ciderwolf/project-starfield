package starfield.data.dao

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import starfield.data.table.Cards
import starfield.data.table.DeckCards
import starfield.data.table.Decks
import starfield.model.toEnum
import starfield.routing.Deck
import starfield.routing.DeckCard
import java.lang.IllegalStateException
import java.util.UUID

class DeckDao {

    enum class StartingZone {
        Main, Side
    }

    private fun mapDeck(deckRow: ResultRow, cards: List<ResultRow>): Deck {
        val main = mutableListOf<DeckCard>()
        val side = mutableListOf<DeckCard>()
        val deckId = deckRow[Decks.id].value
        cards.forEach {
            if (it[DeckCards.deckId] == deckId) {
                when(it[DeckCards.startingZone].toEnum<StartingZone>()) {
                    StartingZone.Main -> main.add(mapCard(it))
                    StartingZone.Side -> side.add(mapCard(it))
                    else -> throw IllegalStateException("Unrecognized state")
                }
            }
        }

        return Deck(deckId, deckRow[Decks.ownerId], deckRow[Decks.name], main, side)
    }

    private fun mapCard(card: ResultRow): DeckCard {
        return DeckCard(
            card[Cards.name],
            card[Cards.type],
            card[Cards.image],
            card[Cards.id],
            card[Cards.backImage],
            card[DeckCards.count].toInt()
        )
    }

    suspend fun createDeck(userId: UUID) = DatabaseSingleton.dbQuery {
        val id = Decks.insertAndGetId {
            it[id] = UUID.randomUUID()
            it[ownerId] = userId
            it[name] = "New Deck"
            it[thumbnailId] = null
        }

        Deck(id.value, userId, "New Deck", listOf(), listOf())
    }

    suspend fun getDeck(deckId: UUID) = DatabaseSingleton.dbQuery {
        Decks.selectAll().where { Decks.id eq deckId }.singleOrNull()?.let {
            val cards = DeckCards.leftJoin(Cards).selectAll().where { DeckCards.deckId eq deckId }
            mapDeck(it, cards.toList())
        }
    }

    suspend fun getDecks(userId: UUID) = DatabaseSingleton.dbQuery {
        val decks = Decks.selectAll().where { Decks.ownerId eq userId }.toList()
        val deckIds = decks.map {it[Decks.id].value }
        val cards = DeckCards.leftJoin(Cards).selectAll().where { DeckCards.deckId inList deckIds }.toList()
        decks.map { mapDeck(it, cards) }
    }

    suspend fun updateDeck(deck: Deck) = DatabaseSingleton.dbQuery {
        Decks.upsert {
            it[id] = deck.id
            it[name] = deck.name
        }
        val allCards = deck.maindeck.map { Pair(it, StartingZone.Main) } +
            deck.sideboard.map { Pair(it, StartingZone.Side) }
        DeckCards.deleteWhere { deckId eq deck.id }
        DeckCards.batchInsert(allCards) {
            val (card, zone) = it
            this[DeckCards.deckId] = deck.id
            this[DeckCards.cardId] = card.id
            this[DeckCards.count] = card.count.toByte()
            this[DeckCards.startingZone] = zone.ordinal.toByte()
        }
    }
}