package starfield.search.filter

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import starfield.data.table.CardParts
import starfield.search.InvalidFilterValueException
import starfield.search.InvalidFilterValueSetException
import starfield.search.SearchOperator

object IsFilter : AbstractFilter(listOf("is")) {
    private val supportedValues = listOf("transform", "aftermath")
    
    override fun buildQuery(operator: SearchOperator, value: String): Op<Boolean> {
        val column = when (value.lowercase()) {
            "transform" -> CardParts.dfc
            "aftermath" -> CardParts.aftermath
            else -> throw InvalidFilterValueSetException(
                "is",
                value,
                supportedValues
            )
        }

        return column eq true
    }

}