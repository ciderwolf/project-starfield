package starfield.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SearchQueryParserTest {

    // ── Helper to build expected trees concisely ──────────────────────────

    private fun cond(
        field: String,
        op: SearchOperator,
        value: String,
        mode: SearchMode = SearchMode.Text
    ) = SearchSyntaxTree.ConditionNode(field, op, SearchSyntaxTree.ValueNode(value, mode))

    private fun nameCond(value: String) = cond("name", SearchOperator.Contains, value)

    // ── Single-word name search ──────────────────────────────────────────

    @Test
    fun `single word is treated as name contains`() {
        val tree = SearchQueryParser.parse("lightning")
        assertEquals(nameCond("lightning"), tree)
    }

    @Test
    fun `quoted multi-word string is treated as name contains`() {
        val tree = SearchQueryParser.parse("\"lightning bolt\"")
        assertEquals(nameCond("lightning bolt"), tree)
    }

    // ── Label (field:value) rules ────────────────────────────────────────

    @Test
    fun `colon operator parses as Contains`() {
        val tree = SearchQueryParser.parse("t:creature")
        assertEquals(cond("t", SearchOperator.Contains, "creature"), tree)
    }

    @Test
    fun `equals operator parses as Equals`() {
        val tree = SearchQueryParser.parse("cmc=3")
        assertEquals(cond("cmc", SearchOperator.Equals, "3"), tree)
    }

    @Test
    fun `greater than operator`() {
        val tree = SearchQueryParser.parse("cmc>3")
        assertEquals(cond("cmc", SearchOperator.GreaterThan, "3"), tree)
    }

    @Test
    fun `less than operator`() {
        val tree = SearchQueryParser.parse("cmc<5")
        assertEquals(cond("cmc", SearchOperator.LessThan, "5"), tree)
    }

    @Test
    fun `greater than or equal operator`() {
        val tree = SearchQueryParser.parse("power>=4")
        assertEquals(cond("power", SearchOperator.GreaterThanOrEqual, "4"), tree)
    }

    @Test
    fun `less than or equal operator`() {
        val tree = SearchQueryParser.parse("toughness<=2")
        assertEquals(cond("toughness", SearchOperator.LessThanOrEqual, "2"), tree)
    }

    @Test
    fun `not equals operator`() {
        val tree = SearchQueryParser.parse("rarity!=common")
        assertEquals(cond("rarity", SearchOperator.NotEquals, "common"), tree)
    }

    @Test
    fun `quoted value in label rule`() {
        val tree = SearchQueryParser.parse("o:\"draw a card\"")
        assertEquals(cond("o", SearchOperator.Contains, "draw a card"), tree)
    }

    // ── Negation ─────────────────────────────────────────────────────────

    @Test
    fun `dash negates a label rule`() {
        val tree = SearchQueryParser.parse("-t:creature")
        assertEquals(
            SearchSyntaxTree.Not(cond("t", SearchOperator.Contains, "creature")),
            tree
        )
    }

    @Test
    fun `dash negation with equals operator`() {
        val tree = SearchQueryParser.parse("-cmc=3")
        assertEquals(
            SearchSyntaxTree.Not(cond("cmc", SearchOperator.Equals, "3")),
            tree
        )
    }

    // ── Implicit AND (juxtaposition) ─────────────────────────────────────

    @Test
    fun `two words are implicitly ANDed`() {
        val tree = SearchQueryParser.parse("lightning bolt")
        assertEquals(
            SearchSyntaxTree.And(nameCond("lightning"), nameCond("bolt")),
            tree
        )
    }

    @Test
    fun `three tokens are left-associated AND`() {
        val tree = SearchQueryParser.parse("t:creature cmc=3 red")
        assertEquals(
            SearchSyntaxTree.And(
                SearchSyntaxTree.And(
                    cond("t", SearchOperator.Contains, "creature"),
                    cond("cmc", SearchOperator.Equals, "3")
                ),
                nameCond("red")
            ),
            tree
        )
    }

    // ── Explicit AND ─────────────────────────────────────────────────────

    @Test
    fun `explicit and keyword`() {
        val tree = SearchQueryParser.parse("t:creature and cmc=3")
        assertEquals(
            SearchSyntaxTree.And(
                cond("t", SearchOperator.Contains, "creature"),
                cond("cmc", SearchOperator.Equals, "3")
            ),
            tree
        )
    }

    // ── Explicit OR ──────────────────────────────────────────────────────

    @Test
    fun `explicit or keyword`() {
        val tree = SearchQueryParser.parse("t:creature or t:planeswalker")
        assertEquals(
            SearchSyntaxTree.Or(
                cond("t", SearchOperator.Contains, "creature"),
                cond("t", SearchOperator.Contains, "planeswalker")
            ),
            tree
        )
    }

    // ── Grouped (parenthesised) rules ────────────────────────────────────

    @Test
    fun `parentheses group rules`() {
        val tree = SearchQueryParser.parse("(t:creature or t:planeswalker) cmc>3")
        assertEquals(
            SearchSyntaxTree.And(
                SearchSyntaxTree.Or(
                    cond("t", SearchOperator.Contains, "creature"),
                    cond("t", SearchOperator.Contains, "planeswalker")
                ),
                cond("cmc", SearchOperator.GreaterThan, "3")
            ),
            tree
        )
    }

    @Test
    fun `nested parentheses`() {
        val tree = SearchQueryParser.parse("((t:creature))")
        assertEquals(cond("t", SearchOperator.Contains, "creature"), tree)
    }

    // ── Mixed operators ──────────────────────────────────────────────────

    @Test
    fun `or has higher precedence than implicit and`() {
        // "a b or c" → a AND (b OR c)  because OR binds first in the grammar
        val tree = SearchQueryParser.parse("t:creature cmc=3 or t:instant")
        // The grammar applies implicit AND to the first two tokens, then OR
        assertIs<SearchSyntaxTree.And>(tree)
    }

    @Test
    fun `multiple label rules with different operators`() {
        val tree = SearchQueryParser.parse("t:creature power>=3 toughness<=4")
        assertEquals(
            SearchSyntaxTree.And(
                SearchSyntaxTree.And(
                    cond("t", SearchOperator.Contains, "creature"),
                    cond("power", SearchOperator.GreaterThanOrEqual, "3")
                ),
                cond("toughness", SearchOperator.LessThanOrEqual, "4")
            ),
            tree
        )
    }

    // ── Keywords as search values ────────────────────────────────────────

    @Test
    fun `and keyword as search subject value`() {
        // e.g. a card whose name contains "and"
        val tree = SearchQueryParser.parse("\"and\"")
        assertEquals(nameCond("and"), tree)
    }

    @Test
    fun `or keyword as search subject value`() {
        val tree = SearchQueryParser.parse("\"or\"")
        assertEquals(nameCond("or"), tree)
    }

    // ── Error handling ───────────────────────────────────────────────────

    @Test
    fun `unmatched open paren throws SearchParseException`() {
        assertFailsWith<SearchParseException> {
            SearchQueryParser.parse("(t:creature")
        }
    }

    @Test
    fun `empty query throws exception`() {
        assertFailsWith<Exception> {
            SearchQueryParser.parse("")
        }
    }

    // ── Complex real-world queries ───────────────────────────────────────

    @Test
    fun `complex query with negation, grouping, and multiple operators`() {
        val tree = SearchQueryParser.parse("-t:land (cmc>=2 or power>3)")
        assertEquals(
            SearchSyntaxTree.And(
                SearchSyntaxTree.Not(cond("t", SearchOperator.Contains, "land")),
                SearchSyntaxTree.Or(
                    cond("cmc", SearchOperator.GreaterThanOrEqual, "2"),
                    cond("power", SearchOperator.GreaterThan, "3")
                )
            ),
            tree
        )
    }

    @Test
    fun `label rule with quoted value and negation`() {
        val tree = SearchQueryParser.parse("-o:\"destroy target\"")
        assertEquals(
            SearchSyntaxTree.Not(cond("o", SearchOperator.Contains, "destroy target")),
            tree
        )
    }

    @Test
    fun `multiple implicit and with negation`() {
        val tree = SearchQueryParser.parse("t:creature -t:human cmc=3")
        assertEquals(
            SearchSyntaxTree.And(
                SearchSyntaxTree.And(
                    cond("t", SearchOperator.Contains, "creature"),
                    SearchSyntaxTree.Not(cond("t", SearchOperator.Contains, "human"))
                ),
                cond("cmc", SearchOperator.Equals, "3")
            ),
            tree
        )
    }
}
