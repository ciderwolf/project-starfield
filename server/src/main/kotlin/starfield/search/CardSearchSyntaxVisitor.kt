package starfield.search

class CardSearchSyntaxVisitor : SearchSyntaxBaseVisitor<SearchSyntaxTree>() {
    override fun visitSearch(ctx: SearchSyntaxParser.SearchContext): SearchSyntaxTree {
        // combine all rules with AND
        var node = visit(ctx.rule_(0))
        for(i in 1 until ctx.rule_().size) {
            node = SearchSyntaxTree.And(node, visit(ctx.rule_(i)))
        }
        return node
    }

    override fun visitSearchSubject(ctx: SearchSyntaxParser.SearchSubjectContext): SearchSyntaxTree.ValueNode {
        if (ctx.QUOTED_STRING() != null) {
            val rawValue = ctx.QUOTED_STRING().text
            val value = rawValue.substring(1, rawValue.length - 1) // Remove quotes
            return SearchSyntaxTree.ValueNode(value, SearchMode.Text)
        } else if (ctx.WORD() != null) {
            val value = ctx.WORD().text
            return SearchSyntaxTree.ValueNode(value, SearchMode.Text)
        }
//        else if (ctx.REGEX() != null) {
//            val rawValue = ctx.REGEX().text
//            val value = rawValue.substring(1, rawValue.length - 1) // Remove slashes
//            return SearchSyntaxTree.ValueNode(value, SearchMode.Regex)
//        }
        else if (ctx.AND() != null) {
            val rawValue = ctx.AND().text
            return SearchSyntaxTree.ValueNode(rawValue, SearchMode.Text)
        } else if (ctx.OR() != null) {
            val rawValue = ctx.OR().text
            return SearchSyntaxTree.ValueNode(rawValue, SearchMode.Text)
        } else {
            throw IllegalArgumentException("Invalid search subject. Expected a single word, or a multi-word search term in quotes.")
        }
    }

    override fun visitGroupedRule(ctx: SearchSyntaxParser.GroupedRuleContext): SearchSyntaxTree {
        var node = visit(ctx.rule_(0))
        for(i in 1 until ctx.rule_().size) {
            node = SearchSyntaxTree.And(node, visit(ctx.rule_(i)))
        }
        return node
    }

    override fun visitNameOnlyRule(ctx: SearchSyntaxParser.NameOnlyRuleContext): SearchSyntaxTree {
        return SearchSyntaxTree.ConditionNode(
            field = "name",
            operator = SearchOperator.Contains,
            value = visitSearchSubject(ctx.searchSubject())
        )

    }

    override fun visitLabelRule(ctx: SearchSyntaxParser.LabelRuleContext): SearchSyntaxTree {
        val condition = SearchSyntaxTree.ConditionNode(
            field = ctx.WORD().text,
            operator = SearchOperator.parse(ctx.SEPERATOR().text),
            value = visitSearchSubject(ctx.searchSubject())
        )
        if (ctx.DASH() != null) {
            return SearchSyntaxTree.Not(condition)
        }
        return condition
    }

    override fun visitOrRule(ctx: SearchSyntaxParser.OrRuleContext): SearchSyntaxTree {
        return SearchSyntaxTree.Or(visit(ctx.rule_(0)), visit(ctx.rule_(1)))
    }

    override fun visitAndRule(ctx: SearchSyntaxParser.AndRuleContext): SearchSyntaxTree {
        return SearchSyntaxTree.And(visit(ctx.rule_(0)), visit(ctx.rule_(1)))
    }
}