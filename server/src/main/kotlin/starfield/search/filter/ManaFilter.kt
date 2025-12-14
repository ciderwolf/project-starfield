package starfield.search.filter

import org.jetbrains.exposed.sql.BooleanColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.booleanLiteral
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.stringLiteral
import starfield.data.table.CardParts
import starfield.search.InvalidFilterValueException
import starfield.search.SearchOperator

object ManaFilter : AbstractFilter(aliases = listOf("m", "mana")) {
    val column = CardParts.manaCost

    override fun buildQuery(operator: SearchOperator, value: String): Op<Boolean> {
        val normalizedValue = normalizeManaString(value)
        val (genericMana, coloredSymbols) = parseManaComponents(normalizedValue)
        
        return when (operator) {
            SearchOperator.Equals -> buildEqualsQuery(genericMana, coloredSymbols)
            SearchOperator.NotEquals -> buildNotEqualsQuery(genericMana, coloredSymbols)
            SearchOperator.GreaterThan -> buildGreaterThanQuery(genericMana, coloredSymbols)
            SearchOperator.LessThan -> buildLessThanQuery(genericMana, coloredSymbols)
            SearchOperator.GreaterThanOrEqual -> buildGreaterThanQuery(genericMana, coloredSymbols) or buildEqualsQuery(genericMana, coloredSymbols)
            SearchOperator.LessThanOrEqual -> buildLessThanQuery(genericMana, coloredSymbols) or buildEqualsQuery(genericMana, coloredSymbols)
            SearchOperator.Contains -> buildGreaterThanQuery(genericMana, coloredSymbols) or buildEqualsQuery(genericMana, coloredSymbols)
        } 
    }

    // Convert shorthand like "2WW" to "{2}{W}{W}"
    private fun normalizeManaString(value: String): String {
        if (value.isEmpty()) {
            throw InvalidFilterValueException("mana", value, "Mana cost cannot be empty.")
        }

        if (value.contains("{")) {
            // Validate brace format
            if (value.count { it == '{' } != value.count { it == '}' }) {
                throw InvalidFilterValueException("mana", value, "Mismatched braces in mana cost.")
            }
            return value.uppercase()
        }
        
        val result = mutableListOf<String>()
        var i = 0
        while (i < value.length) {
            val char = value[i].uppercaseChar()
            if (char.isDigit()) {
                // Collect all consecutive digits
                val numStart = i
                while (i < value.length && value[i].isDigit()) {
                    i++
                }
                result.add("{${value.substring(numStart, i)}}")
            } else if (char.isLetter()) {
                result.add("{$char}")
                i++
            } else if (char == '/') {
                // Handle hybrid mana symbols like "W/U"
                // combine the next character as well with the previous element in result
                if (result.isEmpty()) {
                    throw InvalidFilterValueException("mana", value, "Invalid hybrid mana symbol.")
                }
                val lastSymbol = result.removeAt(result.size - 1)
                i++
                if (i >= value.length || !value[i].isLetter()) {
                    throw InvalidFilterValueException("mana", value, "Invalid hybrid mana symbol.")
                }
                val hybridChar = value[i].uppercaseChar()
                result.add("{${lastSymbol.trim('{', '}')}/$hybridChar}")
                i++
            }
            else {
                throw InvalidFilterValueException("mana", value, "Invalid character '$char' in mana cost")
            }
        }
        
        return result.joinToString("")
    }

    private fun parseManaComponents(manaString: String): Pair<Int, String> {
        val symbols = manaString.trim('{', '}').split("}{")
        var genericMana = 0
        val coloredSymbols = mutableListOf<String>()
        
        for (symbol in symbols) {
            if (symbol.isEmpty()) continue
            
            val numValue = symbol.toIntOrNull()
            if (numValue != null) {
                genericMana += numValue
            } else {
                coloredSymbols.add(symbol)
            }
        }
        
        return Pair(genericMana, coloredSymbols.joinToString(""))
    }

    private fun buildEqualsQuery(genericMana: Int, coloredSymbols: String): Op<Boolean> {
        val dbGenericMana = extractGenericMana(column)
        val dbColoredSymbols = extractColoredSymbols(column)
        
        return (dbGenericMana eq intLiteral(genericMana)) and (dbColoredSymbols eq stringLiteral(coloredSymbols))
    }

    private fun buildNotEqualsQuery(genericMana: Int, coloredSymbols: String): Op<Boolean> {
        val dbGenericMana = extractGenericMana(column)
        val dbColoredSymbols = extractColoredSymbols(column)
        
        return (dbGenericMana neq intLiteral(genericMana)) or (dbColoredSymbols neq stringLiteral(coloredSymbols))
    }

    private fun buildGreaterThanQuery(genericMana: Int, coloredSymbols: String): Op<Boolean> {
        val dbGenericMana = extractGenericMana(column)
        val dbColoredSymbols = extractColoredSymbols(column)
        
        // Greater than if: (generic mana is greater AND colored symbols match the pattern)
        //               OR (generic equal AND colored symbols contain the pattern)
        return ((dbGenericMana greater intLiteral(genericMana)) and coloredContains(dbColoredSymbols, stringLiteral(coloredSymbols), false)) or
               ((dbGenericMana eq intLiteral(genericMana)) and coloredContains(dbColoredSymbols, stringLiteral(coloredSymbols), true))
    }

    private fun buildLessThanQuery(genericMana: Int, coloredSymbols: String): Op<Boolean> {
        val dbGenericMana = extractGenericMana(column)
        val dbColoredSymbols = extractColoredSymbols(column)
        
        // Less than if: generic mana is less OR (generic equal AND pattern contains colored symbols)
        return ((dbGenericMana less intLiteral(genericMana)) and coloredContains(stringLiteral(coloredSymbols), dbColoredSymbols, false)) or
               ((dbGenericMana eq intLiteral(genericMana)) and coloredContains(stringLiteral(coloredSymbols), dbColoredSymbols, true))
    }

    private fun extractGenericMana(manaCostColumn: Column<String>): CustomFunction<Int> {
        // Extract the sum of numeric (generic) mana from a cost string
        return CustomFunction(
            "mana_generic",
            IntegerColumnType(),
            manaCostColumn
        )
    }

    private fun extractColoredSymbols(manaCostColumn: Column<String>): CustomFunction<String> {
        // Extract colored symbols as a sorted string (e.g., "GW" for {G}{W})
        return CustomFunction(
            "mana_colored",
            TextColumnType(),
            manaCostColumn
        )
    }

    private fun coloredContains(container: Expression<String>, contained: Expression<String>, strictSuperset: Boolean): CustomFunction<Boolean> {
        // Check if container contains all symbols in contained
        // If strictSuperset is true, container must have MORE symbols than contained
        return CustomFunction(
            "mana_colored_contains",
            BooleanColumnType(),
            container,
            contained,
            booleanLiteral(strictSuperset)
        )
    }
}
