package starfield.engine

import kotlinx.coroutines.runBlocking
import starfield.data.dao.CardDao
import starfield.routing.DeckCard

class CardInfoProvider(sourceCards: List<DeckCard>) {
    private val cards: MutableMap<OracleId, CardDao.CardEntity>
    private val availableTokens: Map<OracleId, CardDao.Token>

    init {
        val dao = CardDao()
        val (cards, tokens) = runBlocking {
            dao.getCardsWithExtras(sourceCards.map { it.id }.distinct())
        }
        this.cards = cards.associateBy { it.id }.toMutableMap()
        this.availableTokens = tokens.associateBy { it.id }
    }

    operator fun get(id: OracleId) = cards[id]
    fun registerCard(card: CardDao.CardEntity) {
        cards[card.id] = card
    }
}