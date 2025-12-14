package starfield.search.filter

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import starfield.search.InvalidFilterValueException
import starfield.search.SearchOperator

class IntFilter(aliases: List<String>, val column: Column<Int>) : AbstractFilter(aliases) {
    override fun buildQuery(operator: SearchOperator, value: String): Op<Boolean> {
        val intValue = value.toIntOrNull() 
            ?: throw InvalidFilterValueException(
                aliases.first(),
                value,
                "Value must be an integer"
            )
        return when (operator) {
            SearchOperator.Equals -> column eq intValue
            SearchOperator.NotEquals -> column neq intValue
            SearchOperator.GreaterThan -> column greater intValue
            SearchOperator.LessThan -> column less intValue
            SearchOperator.GreaterThanOrEqual -> column greaterEq intValue
            SearchOperator.LessThanOrEqual -> column lessEq intValue
            SearchOperator.Contains -> column eq intValue
        }
    }
}