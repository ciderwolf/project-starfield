package starfield.data.dao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import starfield.Id
import starfield.data.table.Cards
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
            backImage = row[Cards.backImage]
        )
    }

    suspend fun getCards(ids: Iterable<UUID>): List<Card> {
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
        val cards = bulkResponse.body()
            .filter { it != "[" && it != "]" }
            .map { Json.decodeFromString<JsonObject>(it.trim(',')) }
            .filter {
                it["set_type"]!!.jsonPrimitive.content !in listOf("token", "funny", "memorabilia")
                        && !it["type_line"]!!.jsonPrimitive.content.contains("Token")
            }
            .map { parseCard(it) }
            .collect(Collectors.toList())

        DatabaseSingleton.dbQuery {
            Cards.batchReplace(cards) {
                this[Cards.id] = it.id
                this[Cards.name] = it.name
                this[Cards.fuzzyName] = normalizeName(it.name)
                this[Cards.type] = it.type
                this[Cards.image] = it.image
                this[Cards.backImage] = it.backImage
                this[Cards.src] = CardSource.Scryfall.ordinal
            }
        }
        return cards.size
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
            backImage
        )
    }

    private fun normalizeName(name: String): String {
        var normalizedName = name
        if (normalizedName.contains(" // ")) {
            normalizedName = normalizedName.split(" // ")[0]
        }
        return normalizedName.lowercase().replace(Regex("[^\\w ]"), "").trim()
    }

    data class Card(
        val name: String,
        val fuzzyName: String,
        val type: String,
        val id: Id,
        val image: String,
        val backImage: String?
    ) {
        fun toOracleCard(): OracleCard {
            return OracleCard(
                name,
                id,
                backImage != null
            )
        }
    }

    @Serializable
    data class OracleCard (
        val name: String,
        val id: Id,
        val hasBackFace: Boolean
    )

    enum class CardSource {
        Scryfall, Custom
    }

}