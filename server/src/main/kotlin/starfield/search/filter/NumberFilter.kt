package starfield.search.filter

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.regexp
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.castTo
import starfield.search.SearchOperator
import starfield.search.UnsupportedOperatorException

class NumberFilter(aliases: List<String>, val column: Column<String?>) : AbstractFilter(aliases) {
    override fun buildQuery(operator: SearchOperator, value: String): Op<Boolean> {
        val intValue = value.toIntOrNull()
        return if (intValue != null) {
            buildIntQuery(operator, intValue)
        } else {
            buildStringQuery(operator, value)
        }
    }

    fun buildIntQuery(operator: SearchOperator, value: Int): Op<Boolean> {
        val isIntegerPattern = "^-?[0-9]+$"
        val castToInt = column.castTo<Int>(IntegerColumnType())

        val isInteger = column regexp isIntegerPattern
        val comparison = when (operator) {
            SearchOperator.Equals -> castToInt eq value
            SearchOperator.NotEquals -> castToInt neq value
            SearchOperator.GreaterThan -> castToInt greater value
            SearchOperator.LessThan -> castToInt less value
            SearchOperator.GreaterThanOrEqual -> castToInt greaterEq value
            SearchOperator.LessThanOrEqual -> castToInt lessEq value
            SearchOperator.Contains -> castToInt eq value
        }

        return isInteger and comparison
    }

    fun buildStringQuery(operator: SearchOperator, value: String): Op<Boolean> {
        return when (operator) {
            SearchOperator.Equals -> column eq value
            SearchOperator.NotEquals -> column neq value
            SearchOperator.Contains -> column eq value
            else -> throw UnsupportedOperatorException(
                aliases.first(),
                operator,
                listOf(SearchOperator.Equals, SearchOperator.NotEquals, SearchOperator.Contains)
            )
        }
    }
}