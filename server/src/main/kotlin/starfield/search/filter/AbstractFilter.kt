package starfield.search.filter

import org.jetbrains.exposed.sql.Op
import starfield.search.SearchOperator

abstract class AbstractFilter(val aliases: List<String>) {

    fun matches(alias: String): Boolean {
        return aliases.any { it.equals(alias, ignoreCase = true) }
    }

    abstract fun buildQuery(operator: SearchOperator, value: String): Op<Boolean>
}