package starfield.draft

import starfield.data.dao.*
import starfield.model.DraftCard
import java.util.*

abstract class SetInfo(
    val name: String,
    protected val boosterInfo: BoosterConfig,
    val strategyInfo: StrategyInfo?) {

    val cardsPerPack: Int
        get() = boosterInfo.boosters.first().contents.values.sum()

    abstract fun createPack(): List<DraftCard>

    companion object {
        suspend fun create(code: UUID): SetInfo {
            val draftSetDao = DraftSetDao()
            val draftSet = draftSetDao.getDraftSet(code)
                ?: throw IllegalArgumentException("Draft set with code $code not found")

            val cardIds = draftSet.boosterInfo.sheets.values.flatMap { it.cards.keys }.distinct()
            val cardDao = CardDao()
            val cardData = cardDao.getCardPrintings(cardIds)
            val cards = cardData.map { DraftCard(it.name, it.id, it.oracleId, false, it.type, it.manaValue, it.manaCost, it.image, it.backImage) }

            return when (draftSet.boosterInfo.packType) {
                PackAlgorithm.CUBE -> CubeSetInfo(cards.associateBy { it.id }, draftSet.name, draftSet.boosterInfo, draftSet.strategy)
                PackAlgorithm.SHEET_SAMPLING -> SheetsSetInfo(cards.associateBy { it.id }, draftSet.name, draftSet.boosterInfo, draftSet.strategy)
            }
        }
    }
}

class SheetsSetInfo(
    private val cards: Map<UUID, DraftCard>,
    code: String,
    boosterInfo: BoosterConfig,
    strategyInfo: StrategyInfo?
) : SetInfo(code, boosterInfo, strategyInfo) {
    override fun createPack(): List<DraftCard> {
        val packCards = mutableListOf<DraftCard>()
        val pack = WeightedRandom(boosterInfo.boosters.map { Pair(it.weight, it) }).pick()
        pack.contents.entries.sortedBy { -it.value }.map { (sheetName, count) ->
            val sheet = boosterInfo.sheets[sheetName]!!
            val random = WeightedRandom(sheet.cards.map { Pair(it.value, it.key) }, withReplacement = sheet.allowDuplicates ?: false)
            repeat(count) {
                val card = cards[random.pick()]!!
                packCards.add(card.copy(foil = sheet.foil))
            }
        }

        return packCards
    }
}

class CubeSetInfo(
    private val cards: Map<UUID, DraftCard>,
    code: String,
    boosterInfo: BoosterConfig,
    strategyInfo: StrategyInfo?
) : SetInfo(code, boosterInfo, strategyInfo) {

    private val cardsToSample = boosterInfo.sheets.values.first().cards
        .flatMap { (cardId, count) -> (0..count).map { cardId } }
    private val sampler = WeightedRandom(cardsToSample.map { Pair(1, it) }, withReplacement = false)

    override fun createPack(): List<DraftCard> {
        val packCards = mutableListOf<DraftCard>()
        repeat(cardsPerPack) {
            val selection = sampler.pick()
            packCards.add(cards[selection]!!)
        }
        return packCards
    }

}
