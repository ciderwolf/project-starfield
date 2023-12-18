package starfield.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import starfield.Id
import starfield.model.OracleId
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.stream.Collectors

class CardDatabase(private val data: Map<String, DbCard>) : Map<String, CardDatabase.DbCard> by data {

    @Serializable
    data class DbCard(
        @SerialName("n")
        val name: String,
        @SerialName("t")
        val type: String,
        @SerialName("i")
        val id: Id,
        @SerialName("f")
        val image: String,
        @SerialName("b")
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

    operator fun get(oracleId: OracleId): DbCard? {
        return data.values.find { it.id == oracleId }
    }

    companion object {

        private var instance: CardDatabase? = null

        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json { explicitNulls = false }

        fun download() {
            val request = HttpRequest.newBuilder(URI("https://api.scryfall.com/bulk-data/oracle-cards")).build()
            val client = HttpClient.newBuilder()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val data = Json.decodeFromString<Map<String, JsonElement>>(response.body())
            val bulkDataUrl = data["download_uri"]!!.jsonPrimitive.content

            val bulkResponse = client.send(HttpRequest.newBuilder(URI(bulkDataUrl)).build(),
                HttpResponse.BodyHandlers.ofLines())
            val cards = bulkResponse.body()
                .filter { it != "[" && it != "]" }
                .map { Json.decodeFromString<JsonObject>(it.trim(',')) }
                .filter {
                    it["set_type"]!!.jsonPrimitive.content !in listOf("token", "funny", "memorabilia")
                            && !it["type_line"]!!.jsonPrimitive.content.contains("Token")
                }
                .map { parseCard(it) }
                .collect(Collectors.toMap({ it.first }, { it.second }))

            val string = json.encodeToString(cards)
            println(string.length)
            val writer = File("carddb.json").bufferedWriter()
            writer.write(string)
            writer.close()
            instance = CardDatabase(cards)
        }

        fun instance(): CardDatabase {
            if (instance == null) {
                val data = json.decodeFromString<Map<String, DbCard>>(File("carddb.json").readText())
                instance = CardDatabase(data)
            }

            return instance!!
        }

        private fun parseCard(card: JsonObject): Pair<String, DbCard> {
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

            return Pair(
                normalizeName(name),
                DbCard(
                    name,
                    type,
                    id,
                    image,
                    backImage
                )
            )
        }

        fun normalizeName(name: String): String {
            var normalizedName = name
            if (normalizedName.contains(" // ")) {
                normalizedName = normalizedName.split(" // ")[0]
            }
            return normalizedName.lowercase().replace(Regex("[^\\w ]"), "").trim()
        }
    }
}