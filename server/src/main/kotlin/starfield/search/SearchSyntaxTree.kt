package starfield.search

sealed class SearchSyntaxTree {
    data class And(val left: SearchSyntaxTree, val right: SearchSyntaxTree) : SearchSyntaxTree()
    data class Or(val left: SearchSyntaxTree, val right: SearchSyntaxTree) : SearchSyntaxTree()
    data class Not(val node: SearchSyntaxTree) : SearchSyntaxTree()
    data class ConditionNode(val field: String, val operator: SearchOperator, val value: ValueNode) : SearchSyntaxTree()
    data class ValueNode(val value: String, val mode: SearchMode) : SearchSyntaxTree()
}