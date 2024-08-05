package starfield.data.dao

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import starfield.cli.CardExtra
import starfield.data.table.CardExtras
import starfield.data.table.CardSources
import starfield.plugins.Id
import starfield.data.table.Cards
import starfield.data.table.Tokens
import starfield.engine.OracleId

class CardDao {

    private fun mapDbCardSource(row: ResultRow): CardSource {
        return CardSource(
            id = row[CardSources.id].value,
            name = row[CardSources.name],
            code = row[CardSources.code]
        )
    }

    private fun mapDbCard(row: ResultRow): Card {
        return Card(
            name = row[Cards.name],
            fuzzyName = row[Cards.fuzzyName],
            type = row[Cards.type],
            id = row[Cards.id],
            image = row[Cards.image],
            backImage = row[Cards.backImage],
            thumbnailImage = row[Cards.thumbnailImage],
            source = row[Cards.src]
        )
    }

    private fun mapToken(row: ResultRow): Token {
        return Token (
            id = row[Tokens.id],
            name = row[Tokens.name],
            fuzzyName = row[Tokens.fuzzyName],
            colors = row[Tokens.colors],
            superTypes = row[Tokens.superTypes].split(" "),
            subTypes = row[Tokens.subTypes].split(" "),
            text = row[Tokens.text],
            pt = row[Tokens.pt],
            image = row[Tokens.image],
            backImage = row[Tokens.backImage]
        )
    }

    suspend fun getCard(id: OracleId) = DatabaseSingleton.dbQuery {
        Cards.selectAll().where { Cards.id eq id}.singleOrNull()?.let(::mapDbCard)
    }

    suspend fun getCards(ids: Iterable<OracleId>): List<Card> {
        return DatabaseSingleton.dbQuery {
            Cards.selectAll().where { Cards.id inList ids }
                .map(::mapDbCard)
        }
    }

    suspend fun getCardsWithExtras(ids: Iterable<OracleId>) = DatabaseSingleton.dbQuery {
        val cards = Cards.leftJoin(CardExtras).selectAll().where { Cards.id inList ids }
            .map {
                val card = mapDbCard(it)
                @Suppress("SENSELESS_COMPARISON")
                if (it[CardExtras.cardId] != null) {
                    card.extras = CardExtra(
                        id = it[CardExtras.cardId],
                        costs = Json.decodeFromString<List<List<String>>>(it[CardExtras.manaCosts]),
                        tokens = Json.decodeFromString<List<Id>>(it[CardExtras.tokens]),
                    )
                }
                card
            }

        val tokenIds = cards.mapNotNull { it.extras }.flatMap { it.tokens }
        val tokens = getTokens(tokenIds)

        Pair(cards, tokens)
    }



    suspend fun tryFindCards(names: List<String>): Map<String, List<Card>> {
        val fuzzyNames = names.map(::normalizeName)
        val result = DatabaseSingleton.dbQuery {
            Cards.selectAll().where { Cards.fuzzyName inList fuzzyNames }
                .map(::mapDbCard)
                .groupBy { it.fuzzyName }
        }

        return result
    }

    suspend fun searchForCards(name: String): List<Card> = DatabaseSingleton.dbQuery {
        val normalizedName = normalizeName(name)
        Cards.selectAll().where { Cards.fuzzyName like "%${normalizedName}%" }.map(::mapDbCard)
    }

    suspend fun getToken(id: OracleId) = DatabaseSingleton.dbQuery {
        Tokens.selectAll().where { Tokens.id eq id }.single().let(::mapToken)
    }

    private suspend fun getTokens(tokenIds: List<Id>) = DatabaseSingleton.dbQuery {
        Tokens.selectAll().where { Tokens.id inList tokenIds }.map(::mapToken)
    }

    suspend fun searchForTokens(name: String?, color: String?, types: List<String>, text: String?, pt: String?) = DatabaseSingleton.dbQuery {
        val sortedColor = color?.toList()?.sorted()?.joinToString("")
        val columns = mapOf(Tokens.fuzzyName to name, Tokens.colors to sortedColor, Tokens.text to text, Tokens.pt to pt)
        var query = Tokens.selectAll()
        for(column in columns) {
            val param = column.value?.let(::normalizeName)
            if (param != null) {
                query = query.andWhere { column.key like "%${param}%" }
            }
        }

        for(type in types) {
            val normalType = normalizeName(type)
            query = query.andWhere { (Tokens.superTypes like "%$normalType%") or (Tokens.subTypes like "%$normalType%") }
        }

        return@dbQuery query.map(::mapToken)
    }

    suspend fun getSources(keys: Collection<Int>) = DatabaseSingleton.dbQuery {
        CardSources.selectAll().where { CardSources.id inList keys }.map(::mapDbCardSource)
    }

    abstract class CardEntity {
        abstract val id: OracleId

        abstract fun toOracleCard(): OracleCard
    }

    data class Token(
        override val id: Id,
        val name: String,
        val fuzzyName: String,
        val colors: String,
        val superTypes: List<String>,
        val subTypes: List<String>,
        val pt: String?,
        val text: String,
        val image: String,
        val backImage: String?
    ) : CardEntity() {
        override fun toOracleCard() = OracleCard(name, id, image, null)
    }

    data class Card(
        val name: String,
        val fuzzyName: String,
        val type: String,
        override val id: Id,
        val image: String,
        val backImage: String?,
        val thumbnailImage: String,
        val source: Int
    ) : CardEntity() {
        var extras: CardExtra? = null
        override fun toOracleCard() = OracleCard(name, id, image, backImage)
    }

    @Serializable
    data class OracleCard (
        val name: String,
        val id: Id,
        val image: String,
        val backImage: String?
    )

    data class CardSource(
        val id: Int,
        val name: String,
        val code: String
    )

    companion object {
        fun normalizeName(name: String): String {
            var normalizedName = name
            if (normalizedName.contains(" // ")) {
                normalizedName = normalizedName.split(" // ")[0]
            }
            return normalizedName.lowercase().replace(Regex("[^\\w ]"), "").trim()
        }
    }

}