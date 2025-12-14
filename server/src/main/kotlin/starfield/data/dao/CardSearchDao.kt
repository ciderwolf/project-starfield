package starfield.data.dao

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import starfield.data.table.CardParts
import starfield.plugins.Id
import starfield.search.*
import starfield.search.filter.ColorFilter
import starfield.search.filter.IntFilter
import starfield.search.filter.IsFilter
import starfield.search.filter.ManaFilter
import starfield.search.filter.NumberFilter
import starfield.search.filter.RarityFilter
import starfield.search.filter.TextFilter

class CardSearchDao {

    private fun mapCardDetails(row: ResultRow): CardDetail {
        return CardDetail(
            sourceId = row[CardParts.sourceId],
            cardId = row[CardParts.cardId],
            partId = row[CardParts.partId],
            number = row[CardParts.number],
            name = row[CardParts.name],
            fuzzyName = row[CardParts.fuzzyName],
            typeLine = row[CardParts.typeLine],
            colors = row[CardParts.colors],
            oracleText = row[CardParts.oracleText],
            html = row[CardParts.html],
            flavorText = row[CardParts.flavorText],
            power = row[CardParts.power],
            toughness = row[CardParts.toughness],
            loyalty = row[CardParts.loyalty],
            defense = row[CardParts.defense],
            rarity = row[CardParts.rarity],
            manaValue = row[CardParts.manaValue],
            manaCost = row[CardParts.manaCost],
            artist = row[CardParts.artist],
            aftermath = row[CardParts.aftermath],
            dfc = row[CardParts.dfc]
        )
    }

    suspend fun getDetails(id: Id): List<CardDetail> = DatabaseSingleton.dbQuery {
        CardParts
            .selectAll()
            .where { CardParts.cardId eq id }
            .orderBy(CardParts.number)
            .map(::mapCardDetails)
    }

    suspend fun search(query: String, cardSourceId: Int): SearchResult<List<CardDetail>> {
        if (query == "") {
            return SearchResult(
                results = DatabaseSingleton.dbQuery {
                    CardParts.selectAll()
                        .where { CardParts.sourceId eq cardSourceId }
                        .orderBy(Pair(CardParts.number, SortOrder.ASC), Pair(CardParts.partId, SortOrder.ASC))
                        .map(::mapCardDetails)
                        .toList()
                },
                warnings = listOf()
            )
        }
        val warnings = mutableListOf<String>()
        val dbQuery = buildQuery(query, cardSourceId, warnings)

        val results = DatabaseSingleton.dbQuery {
            val cardIds = dbQuery
                .map { it[CardParts.cardId] }
                .distinct()
                .toList()
            CardParts.selectAll()
                .where { CardParts.cardId inList cardIds }
                .orderBy(Pair(CardParts.number, SortOrder.ASC), Pair(CardParts.partId, SortOrder.ASC))
                .map(::mapCardDetails)
                .toList()
        }

        return SearchResult(
            results = results,
            warnings = warnings.toList()
        )
    }

    private fun buildQuery(query: String, cardSourceId: Int, warnings: MutableList<String>): Query {
        val ast = SearchQueryParser.parse(query)

        val condition = addPartToQuery(ast, warnings)
        val query = CardParts.select(CardParts.cardId)
        
        return if (condition != null) {
            query.where { condition and (CardParts.sourceId eq cardSourceId) }
        } else {
            query
        }
    }

    private fun addPartToQuery(ast: SearchSyntaxTree, warnings: MutableList<String>): Op<Boolean>? {
        return try {
            when (ast) {
                is SearchSyntaxTree.And -> {
                    val left = addPartToQuery(ast.left, warnings)
                    val right = addPartToQuery(ast.right, warnings)
                    when {
                        left != null && right != null -> left and right
                        left != null -> left
                        right != null -> right
                        else -> null
                    }
                }
                is SearchSyntaxTree.ConditionNode -> getConditionQuery(ast)
                is SearchSyntaxTree.Not -> {
                    val inner = addPartToQuery(ast.node, warnings)
                    if (inner != null) not(inner) else null
                }
                is SearchSyntaxTree.Or -> {
                    val left = addPartToQuery(ast.left, warnings)
                    val right = addPartToQuery(ast.right, warnings)
                    when {
                        left != null && right != null -> left or right
                        left != null -> left
                        right != null -> right
                        else -> null
                    }
                }
                is SearchSyntaxTree.ValueNode -> {
                    warnings.add("Unexpected value node in query tree")
                    null
                }
            }
        } catch (e: SearchException) {
            e.printStackTrace()
            warnings.add(e.message!!)
            null
        } catch (e: Exception) {
            e.printStackTrace()
            warnings.add("Error processing part of query: ${e.message}")
            null
        }
    }

    private fun getConditionQuery(node: SearchSyntaxTree.ConditionNode): Op<Boolean> {
        val filters = listOf(
            IsFilter,
            ColorFilter,
            RarityFilter,
            ManaFilter,
            NumberFilter(listOf("power", "pow"), CardParts.power),
            NumberFilter(listOf("toughness", "tou"), CardParts.toughness),
            NumberFilter(listOf("loyalty", "loy"), CardParts.loyalty),
            NumberFilter(listOf("defense", "def"), CardParts.defense),
            IntFilter(listOf("manavalue", "cmc", "mv"), CardParts.manaValue),
            TextFilter(listOf("name"), CardParts.fuzzyName),
            TextFilter(listOf("type", "types", "t"), CardParts.typeLine),
            TextFilter(listOf("oracle", "o"), CardParts.oracleText),
            TextFilter(listOf("flavor", "ft"), CardParts.flavorText),
            TextFilter(listOf("artist", "a"), CardParts.artist)
        )

        val filter = filters.firstOrNull { it.matches(node.field) }
            ?: throw UnknownFilterException(
                node.field,
                filters.flatMap { it.aliases }.distinct().sorted()
            )
        
        return filter.buildQuery(node.operator, node.value.value)
    }

    @Serializable
    data class CardDetail(
        val sourceId: Int,
        val cardId: Id,
        val partId: Int,
        val number: Int,
        val name: String,
        val fuzzyName: String,
        val typeLine: String,
        val colors: String,
        val oracleText: String,
        val html: String,
        val flavorText: String,
        val power: String?,
        val toughness: String?,
        val loyalty: String?,
        val defense: String?,
        val rarity: Int,
        val manaValue: Int,
        val manaCost: String,
        val artist: String,
        val aftermath: Boolean,
        val dfc: Boolean
    )
}