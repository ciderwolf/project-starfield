package starfield.search

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException

object SearchQueryParser {
    fun parse(query: String): SearchSyntaxTree {
        try {
            val lexer = SearchSyntaxLexer(CharStreams.fromString(query))
            val tokens = CommonTokenStream(lexer)
            val parser = SearchSyntaxParser(tokens)
            val tree = parser.search()
            val visitor = CardSearchSyntaxVisitor()
            return visitor.visit(tree)
        } catch (e: RecognitionException) {
            throw SearchParseException(
                "Failed to parse search query at position ${e.offendingToken?.startIndex ?: "unknown"}: ${e.message}",
                e
            )
        } catch (e: Exception) {
            throw SearchParseException("Failed to parse search query: ${e.message}", e)
        }
    }
}