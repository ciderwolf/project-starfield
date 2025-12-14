package starfield.search.filter

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.charLength
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.or
import starfield.data.table.CardParts
import starfield.search.InvalidFilterValueSetException
import starfield.search.SearchOperator

object ColorFilter : AbstractFilter(aliases = listOf("c", "color", "colors")) {
    val column = CardParts.colors
    private val allColors = setOf('W', 'U', 'B', 'R', 'G', 'C')

    override fun buildQuery(operator: SearchOperator, value: String): Op<Boolean> {
        val intValue = value.toIntOrNull()
        if (intValue != null) {
            return buildColorCountQuery(operator, intValue)
        } else {
            // Validate color characters
            val invalidChars = value.uppercase().filter { it !in allColors }
            if (invalidChars.isNotEmpty()) {
                throw InvalidFilterValueSetException(
                    "color", value, allColors.map { it.toString() }
                )
            }
            val targetedColors = getTargetedColors(value)
            return buildColorSetQuery(operator, targetedColors)
        }
    }

    private fun buildColorCountQuery(operator: SearchOperator, value: Int): Op<Boolean> {
        return when (operator) {
            SearchOperator.Equals -> column.charLength() eq value
            SearchOperator.NotEquals -> column.charLength() neq value
            SearchOperator.GreaterThan -> column.charLength() greater value
            SearchOperator.LessThan -> column.charLength() less value
            SearchOperator.GreaterThanOrEqual -> column.charLength() greaterEq value
            SearchOperator.LessThanOrEqual -> column.charLength() lessEq value
            SearchOperator.Contains -> column.charLength() eq value
        }
    }

    private fun buildColorSetQuery(operator: SearchOperator, targetedColors: Set<Char>): Op<Boolean> {
        if (targetedColors.isEmpty()) {
            return if (operator == SearchOperator.Equals) {
                (column.isNull()) or (column eq "")
            } else {
                column.isNotNull() and (column neq "")
            }
        }

        return when (operator) {
            SearchOperator.Equals -> buildExactMatch(targetedColors)
            SearchOperator.NotEquals -> not(buildExactMatch(targetedColors))
            SearchOperator.GreaterThan -> buildMoreColorsThan(targetedColors)
            SearchOperator.LessThan -> buildFewerColorsThan(targetedColors)
            SearchOperator.GreaterThanOrEqual -> buildContainsAll(targetedColors)
            SearchOperator.LessThanOrEqual -> buildContainsAny(targetedColors)
            SearchOperator.Contains -> buildContainsAll(targetedColors)
        }
    }

    private fun buildExactMatch(targetedColors: Set<Char>): Op<Boolean> {
        val lengthCheck = column.charLength() eq targetedColors.size
        val containsAll = buildContainsAll(targetedColors)
        return lengthCheck and containsAll
    }

    private fun buildContainsAll(targetedColors: Set<Char>): Op<Boolean> {
        return targetedColors.map<Char, Op<Boolean>> { color ->
            column like "%$color%"
        }.reduceOrNull { acc, op -> acc and op } ?: Op.TRUE
    }

    private fun buildContainsAny(targetedColors: Set<Char>): Op<Boolean> {
        return targetedColors.map<Char, Op<Boolean>> { color ->
            column like "%$color%"
        }.reduceOrNull { acc, op -> acc or op } ?: Op.FALSE
    }

    private fun buildIsSubsetOf(targetedColors: Set<Char>): Op<Boolean> {
        val excludedColors = allColors - targetedColors

        return if (excludedColors.isEmpty()) {
            Op.TRUE
        } else {
            // No excluded colors should be present
            targetedColors.map<Char, Op<Boolean>> { color ->
               column like "%$color%"
            }.reduce { acc, op -> acc and op }
        }
    }

    private fun buildMoreColorsThan(targetedColors: Set<Char>): Op<Boolean> {
        // Card has more colors: length > target length AND is not subset
        return (column.charLength() greater targetedColors.size) and
                buildIsSubsetOf(targetedColors)
    }

    private fun buildFewerColorsThan(targetedColors: Set<Char>): Op<Boolean> {
        // Card has fewer colors: length < target length AND is subset
        return (column.charLength() less targetedColors.size) and
                buildContainsAny(targetedColors)
    }

    fun getTargetedColors(value: String): Set<Char> {
        val colorMap = mapOf(
            "white" to 'W',
            "blue" to 'U',
            "black" to 'B',
            "red" to 'R',
            "green" to 'G',
            "colorless" to 'C',
        )

        if (colorMap.contains(value.lowercase())) {
            return setOf(colorMap[value.lowercase()]!!)
        }

        return value.uppercase().toSet().filter { it in colorMap.values }.toSet()
    }
}