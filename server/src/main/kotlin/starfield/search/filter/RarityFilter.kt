package starfield.search.filter

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import starfield.data.table.CardParts
import starfield.search.InvalidFilterValueException
import starfield.search.SearchOperator

object RarityFilter : AbstractFilter(listOf("r", "rarity")) {
    private val rarityValueMap = mapOf(
        "c" to 0,
        "u" to 1,
        "r" to 2,
        "m" to 3,
        "common" to 0,
        "uncommon" to 1,
        "rare" to 2,
        "mythic" to 3
    )

    override fun buildQuery(operator: SearchOperator, value: String): Op<Boolean> {
        val lowercaseValue = value.lowercase()
        if (!rarityValueMap.containsKey(lowercaseValue)) {
            throw InvalidFilterValueException(
                "rarity",
                value,
                "Invalid rarity. Valid values: ${rarityValueMap.keys.joinToString(", ")}"
            )
        }
        val intValue = rarityValueMap[lowercaseValue]!!
        return when (operator) {
            SearchOperator.Equals -> CardParts.rarity eq intValue
            SearchOperator.NotEquals -> CardParts.rarity neq intValue
            SearchOperator.GreaterThan -> CardParts.rarity greater intValue
            SearchOperator.LessThan -> CardParts.rarity less intValue
            SearchOperator.GreaterThanOrEqual -> CardParts.rarity greaterEq intValue
            SearchOperator.LessThanOrEqual -> CardParts.rarity lessEq intValue
            SearchOperator.Contains -> CardParts.rarity eq intValue
        }
    }
}