package starfield.engine

import kotlinx.coroutines.runBlocking
import starfield.data.dao.CardDao
import starfield.routing.DeckCard

class CardInfoProvider(sourceCards: List<DeckCard>) {
    private val cards: MutableMap<OracleId, CardDao.CardEntity>

    init {
        val dao = CardDao()
        val cards = runBlocking {
            dao.getCards(sourceCards.map { it.id }.distinct())
        }
        this.cards = cards.associateBy { it.id }.toMutableMap()
    }

    operator fun get(id: OracleId) = cards[id]
    fun registerCard(card: CardDao.CardEntity) {
        cards[card.id] = card
    }
}