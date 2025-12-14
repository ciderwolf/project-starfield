package starfield.search

import kotlinx.serialization.Serializable

/**
 * Base exception for all search-related errors
 */
open class SearchException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when the search query has invalid syntax (e.g., mismatched quotes, invalid operators)
 */
class SearchParseException(message: String, cause: Throwable? = null) : SearchException(message, cause)

/**
 * Thrown when a filter alias doesn't match any registered filter
 */
class UnknownFilterException(
    val filterName: String,
    val availableFilters: List<String>
) : SearchException("Unknown filter '$filterName'. Available filters: ${availableFilters.joinToString(", ")}")

/**
 * Thrown when a filter value is invalid for that specific filter
 */
class InvalidFilterValueException(
    val filterName: String,
    val value: String,
    val reason: String
) : SearchException("Invalid value '$value' for filter '$filterName': $reason")

class InvalidFilterValueSetException(
    val filterName: String,
    val value: String,
    val validValues: List<String>
) : SearchException("Invalid value '$value' for filter '$filterName'. Valid values: ${validValues.joinToString(", ")}")


/**
 * Thrown when an operator is not supported for a given filter
 */
class UnsupportedOperatorException(
    val filterName: String,
    val operator: SearchOperator,
    val supportedOperators: List<SearchOperator>
) : SearchException(
    "Operator '$operator' is not supported for filter '$filterName'. " +
    "Supported operators: ${supportedOperators.joinToString(", ")}"
)

/**
 * Container for a search result with potential warnings
 */
@Serializable
data class SearchResult<T>(
    val results: T,
    val warnings: List<String> = emptyList()
) {
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}
