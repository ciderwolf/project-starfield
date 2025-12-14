package starfield.search

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

object SearchQueryParser {
    fun parse(query: String): SearchSyntaxTree {
        val lexer = SearchSyntaxLexer(CharStreams.fromString(query))
        val tokens = CommonTokenStream(lexer)
        val parser = SearchSyntaxParser(tokens)
        parser.addErrorListener(CardSearchSyntaxErrorListener())
        val tree = parser.search()
        val visitor = CardSearchSyntaxVisitor()
        return visitor.visit(tree)
    }
}