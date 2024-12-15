package starfield.data.dao

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import starfield.data.table.CardSources
import starfield.data.table.Cards
import starfield.data.table.DeckCards
import starfield.data.table.Decks
import starfield.plugins.Id
import starfield.plugins.toEnum
import starfield.routing.Deck
import starfield.routing.DeckCard
import java.lang.IllegalStateException
import java.util.UUID

class DeckDao {

    enum class StartingZone {
        Main, Side
    }

    enum class ConflictResolutionStrategy {
        NoConflict, Default, Best, Pinned
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

        return Deck(deckId, deckRow[Decks.ownerId], deckRow[Decks.name], deckRow[Cards.thumbnailImage], main, side)
    }

    private fun mapCard(card: ResultRow): DeckCard {
        return DeckCard(
            card[Cards.name],
            card[Cards.type],
            card[Cards.image],
            card[Cards.id],
            card[Cards.backImage],
            card[DeckCards.count].toInt(),
            card[CardSources.code],
            card[DeckCards.conflictResolutionStrategy].toEnum<ConflictResolutionStrategy>()!!
        )
    }

    suspend fun createDeck(userId: UUID) = DatabaseSingleton.dbQuery {
        val id = Decks.insertAndGetId {
            it[id] = UUID.randomUUID()
            it[ownerId] = userId
            it[name] = "New Deck"
            it[thumbnailId] = null
        }

        Deck(id.value, userId, "New Deck", null, listOf(), listOf())
    }

    suspend fun createDeck(userId: UUID, deckName: String, maindeck: List<Pair<Int, UUID>>) = DatabaseSingleton.dbQuery {
        val id = Decks.insertAndGetId {
            it[id] = UUID.randomUUID()
            it[ownerId] = userId
            it[name] = deckName
            it[thumbnailId] = maindeck.first().second
        }

        DeckCards.batchInsert(maindeck) {
            val (count, card) = it
            this[DeckCards.deckId] = id.value
            this[DeckCards.cardId] = card
            this[DeckCards.count] = count.toByte()
            this[DeckCards.startingZone] = StartingZone.Main.ordinal.toByte()
            this[DeckCards.conflictResolutionStrategy] = ConflictResolutionStrategy.NoConflict.ordinal.toByte()
        }
        return@dbQuery id.value
    }

    suspend fun getDeck(deckId: UUID) = DatabaseSingleton.dbQuery {
        Decks.leftJoin(Cards).selectAll().where { Decks.id eq deckId }.singleOrNull()?.let {
            val cards = DeckCards.leftJoin(Cards).innerJoin(CardSources).selectAll().where { DeckCards.deckId eq deckId }
            mapDeck(it, cards.toList())
        }
    }

    suspend fun getDecks(userId: UUID) = DatabaseSingleton.dbQuery {
        val decks = Decks.leftJoin(Cards).selectAll().where { Decks.ownerId eq userId }.toList()
        val deckIds = decks.map {it[Decks.id].value }
        val cards = DeckCards.leftJoin(Cards).innerJoin(CardSources).selectAll().where { DeckCards.deckId inList deckIds }.toList()
        decks.map { mapDeck(it, cards) }
    }

    suspend fun updateDeck(id: Id, name: String, thumbnailId: Id?, maindeck: List<DeckCard>, sideboard: List<DeckCard>) = DatabaseSingleton.dbQuery {
        Decks.update({ Decks.id eq id }) {
            it[Decks.name] = name
            it[Decks.thumbnailId] = thumbnailId
        }
        val allCards = maindeck.map { Pair(it, StartingZone.Main) } +
            sideboard.map { Pair(it, StartingZone.Side) }
        DeckCards.deleteWhere { deckId eq id }
        DeckCards.batchInsert(allCards) {
            val (card, zone) = it
            this[DeckCards.deckId] = id
            this[DeckCards.cardId] = card.id
            this[DeckCards.count] = card.count.toByte()
            this[DeckCards.startingZone] = zone.ordinal.toByte()
            this[DeckCards.conflictResolutionStrategy] = card.conflictResolutionStrategy.ordinal.toByte()
        }
    }

    suspend fun deleteDeck(deckId: UUID, userId: UUID) = DatabaseSingleton.dbQuery {
        if (getDeck(deckId)?.owner != userId) {
            return@dbQuery 0
        }
        DeckCards.deleteWhere { DeckCards.deckId eq deckId }
        Decks.deleteWhere { Decks.id eq deckId }
    }
}