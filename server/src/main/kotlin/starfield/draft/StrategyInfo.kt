package starfield.draft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import starfield.plugins.Id
import java.util.*

@Serializable
data class StrategyInfo(
    val cards: List<BotCard>,
    @SerialName("target_curve")
    val targetCurve: Map<Int, Int>,
    @SerialName("archetype_colors")
    val archetypeColors: Int,
    @SerialName("mana_fixers")
    val manaFixers: List<Id>,
    @SerialName("target_fixing")
    val targetFixing: Double,
    @SerialName("color_combinations")
    val colorCombinations: List<List<String>>
) {
    @Serializable
    data class BotCard(
        val id: Id,
        val cmc: Int,
        val colors: List<String>,
        val type: List<String>,
        @SerialName("synergy_score")
        val synergyScore: Double)

    fun botCard(id: UUID): BotCard {
        return cards.find { it.id == id }!!
    }

    fun avgRatingAtManaCost(manaCost: Int): Double {
        val cards = this.cards.filter { it.cmc == manaCost }
        if (cards.isEmpty()) {
            return 0.0
        }
        return cards.sumOf { it.synergyScore } / cards.size
    }
}