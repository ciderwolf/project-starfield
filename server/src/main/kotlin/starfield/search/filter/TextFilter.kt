package starfield.search.filter

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.concat
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.stringLiteral
import starfield.data.table.CardParts
import starfield.search.SearchOperator
import starfield.search.UnsupportedOperatorException

class TextFilter(aliases: List<String>, val column: Column<String>) : AbstractFilter(aliases) {
    override fun buildQuery(operator: SearchOperator, value: String): Op<Boolean> {

        val normalizedValue = CustomFunction<String>(
            "REPLACE",
            TextColumnType(),
            stringLiteral(value.lowercase()), CardParts.name.lowerCase(), stringLiteral("~"))
        val pattern = concat(stringLiteral("%"), normalizedValue, stringLiteral("%"))

        return when (operator) {
            SearchOperator.Equals -> column.lowerCase() like pattern
            SearchOperator.NotEquals -> not(column.lowerCase() like pattern)
            SearchOperator.Contains -> column.lowerCase() like pattern
            else -> throw UnsupportedOperatorException(aliases.first(), operator, listOf(SearchOperator.Equals,
                SearchOperator.NotEquals, SearchOperator.Contains))
        }
    }
}