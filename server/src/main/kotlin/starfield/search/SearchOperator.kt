package starfield.search

enum class SearchOperator {
    Equals,
    NotEquals,
    GreaterThan,
    LessThan,
    GreaterThanOrEqual,
    LessThanOrEqual,
    Contains;

    override fun toString(): String {
        return when (this) {
            Equals -> "="
            NotEquals -> "!="
            GreaterThan -> ">"
            LessThan -> "<"
            GreaterThanOrEqual -> ">="
            LessThanOrEqual -> "<="
            Contains -> ":"
        }
    }

    companion object {
        fun parse(op: String): SearchOperator {
            return when (op) {
                "=" -> Equals
                "!=" -> NotEquals
                ">" -> GreaterThan
                "<" -> LessThan
                ">=" -> GreaterThanOrEqual
                "<=" -> LessThanOrEqual
                ":" -> Contains
                else -> throw IllegalArgumentException("Invalid operator: $op")
            }
        }
    }
}
