package starfield.engine

import kotlinx.coroutines.runBlocking
import starfield.data.dao.CardDao
import starfield.routing.DeckCard

class CardInfoProvider(sourceCards: List<DeckCard>) {
    private val cards: MutableMap<OracleId, CardDao.CardEntity>
    private val availableTokens: MutableMap<OracleId, CardDao.Token>

    init {
        val dao = CardDao()
        val (cards, tokens) = runBlocking {
            dao.getCardsWithExtras(sourceCards.map { it.id }.distinct())
        }
        this.cards = cards.associateBy { it.id }.toMutableMap()
        this.availableTokens = tokens.associateBy { it.id }.toMutableMap()
    }

    operator fun get(id: OracleId) = cards[id]
    private fun registerCard(card: CardDao.CardEntity) {
        cards[card.id] = card
    }

    fun getToken(token: OracleId): CardDao.Token? {
        return availableTokens[token]
    }

    suspend fun tryRegisterCard(id: OracleId): CardDao.CardEntity {
        if (id in cards) {
            return cards.getValue(id)
        }

        val dao = CardDao()
        val (cards, tokens) = dao.getCardsWithExtras(listOf(id))
        if (cards.isEmpty()) {
            throw IllegalStateException("Card not found: $id")
        }
        registerCard(cards[0])
        tokens.forEach { availableTokens[it.id] = it }
        return cards[0]
    }

    suspend fun tryRegisterToken(id: OracleId): CardDao.CardEntity {
        if (id in cards) {
            return cards.getValue(id)
        } else if (id in availableTokens) {
            val token = availableTokens.getValue(id)
            registerCard(token)
            return token
        }

        val dao = CardDao()
        val token = dao.getToken(id)
        registerCard(token)
        return token
    }
}