package starfield.cli

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import starfield.data.dao.CardDao
import starfield.plugins.Id
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*

val manaCostRegex = Regex("(?:\\{[1-9WUBRGCXYZS/]})+")
val excludeCostsRegex = Regex("(costs? ${manaCostRegex.pattern} (more|less))|(add ${manaCostRegex.pattern})|(${
    manaCostRegex.pattern} was (spent|paid))", RegexOption.IGNORE_CASE)

@Serializable
data class CardExtra(val id: Id, val costs: List<List<String>>, val tokens: List<Id>) {
    fun isEmpty(): Boolean {
        return costs.isEmpty() && tokens.isEmpty()
    }
}

fun parseCardExtras(card: JsonObject, id: Id, tokenIds: Map<Id, CardDao.Printing>): CardExtra {
    val manaCosts = mutableListOf<List<String>>()
    val tokens = mutableListOf<Id>()

    if ("card_faces" in card) {
        for(face in card["card_faces"]!!.jsonArray) {
            if ("mana_cost" in face.jsonObject) {
                manaCosts.add(parseCost(face.jsonObject["mana_cost"]!!.jsonPrimitive.content))
            }
            val oracleText = face.jsonObject["oracle_text"]!!.jsonPrimitive.content
            manaCosts.addAll(findManaCosts(oracleText))
        }
    } else {
        if ("mana_cost" in card) {
            manaCosts.add(parseCost(card["mana_cost"]!!.jsonPrimitive.content))
        }
        val oracleText = card["oracle_text"]!!.jsonPrimitive.content
        manaCosts.addAll(findManaCosts(oracleText))
    }

    if ("all_parts" in card) {
        for (part in card["all_parts"]!!.jsonArray) {
            if (part.jsonObject["component"]!!.jsonPrimitive.content == "token") {
                val scryfallId = UUID.fromString(part.jsonObject["id"]!!.jsonPrimitive.content)
                val tokenId = tokenIds[scryfallId]
                if (tokenId == null) {
//                    println("Token not found for $scryfallId")
                    continue
                }
                tokens.add(tokenId.cardId)
            }
        }
    }

    return CardExtra(id, manaCosts.filter { it.isNotEmpty() }.distinct(), tokens)
}

fun findManaCosts(oracleText: String): List<List<String>> {
    return oracleText.split("\n")
        .filter { !excludeCostsRegex.containsMatchIn(it) }
        .flatMap { line ->
            manaCostRegex.findAll(line)
                .map { it.value }
                .map { parseCost(it) }
                .toList()
        }
        .filter { it.isNotEmpty() }
}

fun parseCost(cost: String): List<String> {
    return cost.trim('}', '{').split("}{").filter { it.isNotEmpty() }
}

suspend fun downloadTokenIdMap(): Map<UUID, CardDao.Printing> {
    // Step 1: Get the response from https://api.scryfall.com/bulk-data/default-cards
    val bulkDataUrl = URL("https://api.scryfall.com/bulk-data/default-cards")
    val bulkDataResponse = withContext(Dispatchers.IO) {
        bulkDataUrl.openStream().use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
        }
    }

    // Step 2: Get the download_uri from the response
    val bulkData = Json.decodeFromString<Map<String, JsonElement>>(bulkDataResponse)
    val downloadUri = bulkData["download_uri"]!!.jsonPrimitive.content

    // Step 3: Download the file from the download_uri
    val cardsJson = withContext(Dispatchers.IO) {
        val downloadUrl = URL(downloadUri)
        downloadUrl.openStream().use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
        }
    }

    // Step 4: Parse the file and create a map from `id` to `oracle_id`
    val cards = Json.decodeFromString<JsonArray>(cardsJson)
    return cards
        .filter {
            "oracle_id" in it.jsonObject
                    && ("image_uris" in it.jsonObject || "card_faces" in it.jsonObject)
                    && it.jsonObject["image_status"]?.jsonPrimitive?.content != "missing"
        }
        .associate {
            val id = UUID.fromString(it.jsonObject["id"]!!.jsonPrimitive.content)
            val oracleId = UUID.fromString(it.jsonObject["oracle_id"]!!.jsonPrimitive.content)

            val card = it.jsonObject
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

            val printing = CardDao.Printing(
                id = id,
                cardId = oracleId,
                image = image,
                backImage = backImage,
                thumbnailImage = thumbnailImage,
                source = 0
            )
            return@associate (id to printing)

        }
}
