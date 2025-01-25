package starfield.draft

import starfield.data.dao.BoosterConfig
import starfield.data.dao.CardDao
import starfield.data.dao.MtgJsonDao
import starfield.model.DraftCard
import java.util.*

class SetInfo(
    val code: String,
    private val cards: Map<UUID, DraftCard>,
    private val boosterInfo: BoosterConfig,
    val strategyInfo: StrategyInfo?) {

    val cardsPerPack: Int
        get() = boosterInfo.boosters.first().contents.values.sum()

    fun createPack(): List<DraftCard> {
        val packCards = mutableListOf<DraftCard>()
        val pack = WeightedRandom(boosterInfo.boosters.map { Pair(it.weight, it) }).pick()
        pack.contents.map { it }.sortedBy { -it.value }.map { (sheetName, count) ->
            val sheet = boosterInfo.sheets[sheetName]!!
            val random = WeightedRandom(sheet.cards.map { Pair(it.value, it.key) }, withReplacement = sheet.allowDuplicates ?: false)
            repeat(count) {
                val card = cards[random.pick()]!!
                packCards.add(card.copy(foil = sheet.foil))
            }
        }

        return packCards
    }

    companion object {
        suspend fun create(code: String): SetInfo {
            val setInfoDao = MtgJsonDao()
            val boosterInfo = setInfoDao.getBoosterInfo(code)
            val strategy = setInfoDao.getDraftStrategy(code)
            val cardIds = boosterInfo.sheets.values.flatMap { it.cards.keys }.distinct()
            val cardDao = CardDao()
            val cardData = cardDao.getCards(cardIds)
            val cards = cardData.map { DraftCard(it.name, it.id, false, it.image, it.backImage) }
            return SetInfo(code, cards.associateBy { it.id }, boosterInfo, strategy)
        }
    }
}
