package starfield.data.dao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import starfield.plugins.Id
import starfield.data.table.Cards
import starfield.data.table.Tokens
import starfield.engine.OracleId
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.stream.Collectors

class CardDao {

    private fun mapDbCard(row: ResultRow): Card {
        return Card(
            name = row[Cards.name],
            fuzzyName = row[Cards.fuzzyName],
            type = row[Cards.type],
            id = row[Cards.id],
            image = row[Cards.image],
            backImage = row[Cards.backImage],
            thumbnailImage = row[Cards.thumbnailImage]
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

    suspend fun tryFindCards(names: List<String>): List<Card?> {
        val fuzzyNames = names.map(::normalizeName)
        val result = DatabaseSingleton.dbQuery {
            Cards.selectAll().where { Cards.fuzzyName inList fuzzyNames }
                .map(::mapDbCard)
                .associateBy { it.fuzzyName }
        }

        return fuzzyNames.map { result[it] }
    }

    suspend fun searchForCards(name: String): List<Card> = DatabaseSingleton.dbQuery {
        val normalizedName = normalizeName(name)
        Cards.selectAll().where { Cards.fuzzyName like "%${normalizedName}%" }.map(::mapDbCard)
    }

    suspend fun getToken(id: OracleId) = DatabaseSingleton.dbQuery {
        Tokens.selectAll().where { Tokens.id eq id }.single().let(::mapToken)
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

    suspend fun download(): Int {
        val request = HttpRequest.newBuilder(URI("https://api.scryfall.com/bulk-data/oracle-cards")).build()
        val client = HttpClient.newBuilder()
            .build()

        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        val data = Json.decodeFromString<Map<String, JsonElement>>(response.body())
        val bulkDataUrl = data["download_uri"]!!.jsonPrimitive.content

        val bulkResponse = withContext(Dispatchers.IO) {
            client.send(
                HttpRequest.newBuilder(URI(bulkDataUrl)).build(),
                HttpResponse.BodyHandlers.ofLines()
            )
        }
        val allCardEntities = bulkResponse.body()
            .filter { it != "[" && it != "]" }
            .map { Json.decodeFromString<JsonObject>(it.trim(',')) }
            .filter {
                it["set_type"]!!.jsonPrimitive.content !in listOf("funny", "memorabilia")
                     //&& !it["type_line"]!!.jsonPrimitive.content.contains("Token")
            }
            .map { parseScryfallEntity(it) }
            .collect(Collectors.toList())

        val cards = allCardEntities.filterIsInstance<Card>()
        val tokens = allCardEntities.filterIsInstance<Token>()

        DatabaseSingleton.dbQuery {
            Cards.batchUpsert(cards) {
                this[Cards.id] = it.id
                this[Cards.name] = it.name
                this[Cards.fuzzyName] = normalizeName(it.name)
                this[Cards.type] = it.type
                this[Cards.image] = it.image
                this[Cards.backImage] = it.backImage
                this[Cards.src] = CardSource.Scryfall.ordinal
            }
        }

        DatabaseSingleton.dbQuery {
            Tokens.deleteAll()
            Tokens.batchInsert(tokens) {
                this[Tokens.id] = it.id
                this[Tokens.name] = it.name
                this[Tokens.fuzzyName] = it.fuzzyName
                this[Tokens.colors] = it.colors
                this[Tokens.superTypes] = it.superTypes.joinToString(" ")
                this[Tokens.subTypes] = it.subTypes.joinToString(" ")
                this[Tokens.text] = it.text
                this[Tokens.pt] = it.pt
                this[Tokens.image] = it.image
                this[Tokens.backImage] = it.backImage
            }
        }

        return cards.size
    }

    private fun parseScryfallEntity(card: JsonObject): CardEntity {
        return if (card["set_type"]!!.jsonPrimitive.content == "token" || card["type_line"]!!.jsonPrimitive.content.contains("Token")) {
            parseToken(card)
        } else {
            parseCard(card)
        }
    }

    private fun parseToken(card: JsonObject): Token {
        val id = UUID.fromString(card["id"]!!.jsonPrimitive.content)
        val name = card["name"]!!.jsonPrimitive.content

        val types = card["type_line"]!!.jsonPrimitive.content.split(" // ")
            .map { it.split(" — ") }
            .map { Pair(it[0].trim().split(" "), it.getOrElse(1) { "" }.trim().split(" ")) }
            .reduce { acc, pair -> Pair(acc.first + pair.first, acc.second + pair.second) }
        val superTypes = types.first.distinct().map(::normalizeName)
        val subTypes = types.second.distinct().map(::normalizeName)
        val pt = if ("power" in card && "toughness" in card) {
            card["power"]!!.jsonPrimitive.content + "/" + card["toughness"]!!.jsonPrimitive.content
        } else {
            null
        }
        val text = if ("oracle_text" in card) {
            card["oracle_text"]!!.jsonPrimitive.content
        } else {
            val faces =card["card_faces"]!!.jsonArray
            faces[0].jsonObject["oracle_text"]!!.jsonPrimitive.content +
                    " // " +
                    faces[1].jsonObject["oracle_text"]!!.jsonPrimitive.content
        }

        val image = if ("image_uris" !in card) {
            card["card_faces"]!!
                .jsonArray[0]
                .jsonObject["image_uris"]!!
                .jsonObject["normal"]!!
                .jsonPrimitive.content
        } else {
            card["image_uris"]!!
                .jsonObject["normal"]!!
                .jsonPrimitive.content
        }

        val backImage = if ("image_uris" !in card) {
            card["card_faces"]!!
                .jsonArray[1]
                .jsonObject["image_uris"]!!
                .jsonObject["normal"]!!
                .jsonPrimitive.content
        } else {
            null
        }

        val colors = card["color_identity"]!!
            .jsonArray.let {
                if (it.size == 0) {
                    "C"
                } else {
                    it.map { c -> c.jsonPrimitive.content }
                        .distinct().sorted().joinToString("")
                }
            }

        return Token(
            id,
            name,
            normalizeName(name),
            colors.lowercase(),
            superTypes,
            subTypes,
            pt?.let(::normalizeName),
            normalizeName(text),
            image,
            backImage,
        )
    }

    private fun parseCard(card: JsonObject): Card {
        val name = card["name"]!!.jsonPrimitive.content

        val type = card["type_line"]!!.jsonPrimitive.content.split(" — ")[0].split(" ").last().trim()

        val image = if ("image_uris" !in card) {
            card["card_faces"]!!
                .jsonArray[0]
                .jsonObject["image_uris"]!!
                .jsonObject["normal"]!!
                .jsonPrimitive.content
        } else {
            card["image_uris"]!!
                .jsonObject["normal"]!!
                .jsonPrimitive.content
        }

        val thumbnailImage = if ("image_uris" !in card) {
            card["card_faces"]!!
                .jsonArray[0]
                .jsonObject["image_uris"]!!
                .jsonObject["art_crop"]!!
                .jsonPrimitive.content
        } else {
            card["image_uris"]!!
                .jsonObject["art_crop"]!!
                .jsonPrimitive.content
        }

        val backImage = if ("image_uris" !in card) {
            card["card_faces"]!!
                .jsonArray[1]
                .jsonObject["image_uris"]!!
                .jsonObject["normal"]!!
                .jsonPrimitive.content
        } else {
            null
        }

        val id = UUID.fromString(card["id"]!!.jsonPrimitive.content)

        return Card(
            name,
            normalizeName(name),
            type,
            id,
            image,
            backImage,
            thumbnailImage
        )
    }

    private fun normalizeName(name: String): String {
        var normalizedName = name
        if (normalizedName.contains(" // ")) {
            normalizedName = normalizedName.split(" // ")[0]
        }
        return normalizedName.lowercase().replace(Regex("[^\\w ]"), "").trim()
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
        val thumbnailImage: String
    ) : CardEntity() {
        override fun toOracleCard() = OracleCard(name, id, image, backImage)
    }

    @Serializable
    data class OracleCard (
        val name: String,
        val id: Id,
        val image: String,
        val backImage: String?
    )

    enum class CardSource {
        Scryfall, Custom
    }

}