package starfield.draft

import starfield.model.Draft
import starfield.model.DraftCard
import starfield.draft.StrategyInfo.BotCard
import kotlin.math.max
import kotlin.math.min

class ColorPieDrafter(private val setInfo: StrategyInfo, draft: Draft) : BotDraftingAgent(draft) {
    private val pickedCards = mutableListOf<BotCard>()
    private val manaCurve = DefaultDict<Int, Int> { 0 }
    private val colorDistribution = mutableMapOf("W" to 0, "U" to 0, "B" to 0, "R" to 0, "G" to 0)

    override fun makePick(pack: Pack): DraftCard {
        val choiceIndex = pack.cards
            .map { setInfo.botCard(it.id) }
            .mapIndexed { index, card -> index to evaluateCard(card) }
            .maxBy { it.second.score }.first
        val choice = setInfo.botCard(pack.cards[choiceIndex].id)
        pickedCards.add(choice)
        println("Now have ${pickedCards.size} cards")
        manaCurve[choice.cmc] += 1
        choice.colors.forEach { colorDistribution[it] = colorDistribution[it]!! + 1 }

        return pack.cards[choiceIndex]
    }

    private fun evaluateCard(card: BotCard): Scores {
        val baseScore = card.synergyScore
        val fixingScore = evaluateManaFixing(card)
        val curveScore = evaluateManaCurve(card)
        val colorScore = evaluateColorScore(card)
        return Scores(baseScore, fixingScore, curveScore, colorScore)
    }

    private fun evaluateManaFixing(card: BotCard): Double {
        if (!setInfo.manaFixers.contains(card.id)) {
            return 0.0
        }

        val fixersSoFar = pickedCards.filter { setInfo.manaFixers.contains(it.id) }.size
        val targetFixing = setInfo.targetFixing

        return max(0.0, (targetFixing - fixersSoFar) / targetFixing)
    }

    private fun evaluateColorScore(card: BotCard): Double {
        if (pickedCards.size < 3) {
            return 0.0
        }

        val targetArchetypes = findMatchingArchetypes()
        if (targetArchetypes.isEmpty()) {
            return 0.0
        }

        return targetArchetypes.flatMap { archetype ->
            card.colors.map {
                if (archetype.contains(it)) {
                    2.0
                } else {
                    -1.0
                }
            }
        }.sum()
    }

    private fun findMatchingArchetypes(): List<List<String>> {
        val archetypeScores = setInfo.colorCombinations.map { combo ->
            combo to combo.sumOf { colorDistribution[it] ?: 0 }
        }
        val bestScore = archetypeScores.maxBy { it.second }.second
        val bestArchetypes = archetypeScores.filter { it.second == bestScore }
        return bestArchetypes.map { it.first }
    }

    private fun evaluateManaCurve(card: BotCard): Double {
        pickedCards.add(card)
        val predictedCurve = projectFutureCurve()
        var totalScore = 0.0

        for ((manaCost, predictedCount) in predictedCurve.entries) {
            val endDeckCount = minOf(predictedCount, setInfo.targetCurve[manaCost] ?: 0)
            if (endDeckCount == 0) continue

            var bestScores = pickedCards.filter { it.cmc == manaCost }.map { it.synergyScore }.toMutableList()
            while (bestScores.size < predictedCount) {
                bestScores.add(setInfo.avgRatingAtManaCost(manaCost))
            }

            bestScores = topN(endDeckCount, bestScores) { it }
            totalScore += bestScores.sum()
        }

        pickedCards.removeAt(pickedCards.size - 1)
        return totalScore
    }

    private fun projectFutureCurve(): DefaultDict<Int, Int> {
        val picksLeft = draft.set.cardsPerPack - pickedCards.size
        val cardsSeen = CARDS_SEEN_LOOKUP.take(max(picksLeft, 0)).sum() * setInfo.archetypeColors / 5
        val totalSeenCurve = setInfo.targetCurve.values.sum()

        val expectedSeenCurve = IntArray(setInfo.targetCurve.size)
        for (manaCost in setInfo.targetCurve.keys) {
            expectedSeenCurve[manaCost] = (cardsSeen * setInfo.targetCurve[manaCost]!! / totalSeenCurve)
        }

        val currentCurve = manaCurve.copyOf()
        val curveDiff = IntArray(setInfo.targetCurve.size)
        for (manaCost in setInfo.targetCurve.keys) {
            if (setInfo.targetCurve[manaCost]!! > currentCurve[manaCost]) {
                curveDiff[manaCost] = setInfo.targetCurve[manaCost]!! - currentCurve[manaCost]
            }
        }

        for (i in 0 until picksLeft) {
            val validManaCosts = curveDiff.indices.filter { expectedSeenCurve[it] > 0 }
            if (validManaCosts.isEmpty()) break

            val worstManaCost = validManaCosts.maxByOrNull { curveDiff[it] } ?: break

            if (curveDiff[worstManaCost] <= 0) break
            currentCurve[worstManaCost] = currentCurve[worstManaCost] + 1
            curveDiff[worstManaCost] -= 1
            expectedSeenCurve[worstManaCost] -= 1
        }

        return currentCurve
    }

    private fun<T> topN(n: Int, list: List<T>, selector: (T) -> Double): MutableList<T> {
        return list.sortedByDescending(selector).take(n).toMutableList()
    }

    private val CARDS_SEEN_LOOKUP = List(15) { 15 - it } + List(15) { 15 - it } + List(15) { 15 - it }

    data class Scores(val baseScore: Double, val fixingScore: Double, val curveScore: Double, var colorScore: Double) {
        val score: Double
            get() = baseScore + fixingScore + curveScore + colorScore
    }
}