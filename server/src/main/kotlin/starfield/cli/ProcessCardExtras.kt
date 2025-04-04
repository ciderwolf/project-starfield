package starfield.cli

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import starfield.cli.scryfall.Card
import starfield.data.dao.CardDao
import starfield.plugins.Id
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*

val manaCostRegex = Regex("(?:\\{[1-9WUBRGCXYZS/]})+")
val excludeCostsRegex = Regex("(costs? ${manaCostRegex.pattern} (more|less))|(add ${manaCostRegex.pattern})|(${
    manaCostRegex.pattern} was (spent|paid))", RegexOption.IGNORE_CASE)

val entersTappedRegex = Regex("^This \\w+ enters tapped\\.$", RegexOption.MULTILINE)
val startingCountersPattern = Regex("^This \\w+ enters with (a|an|one|two|three|four|five|six|seven|eight|nine|ten) [+/\\w]+ counters? on it\\.$", RegexOption.MULTILINE)

@Serializable
data class CardExtra(val id: Id, val costs: List<List<String>>, val tokens: List<Id>, val isSideways: Boolean = false, val entersTapped: Boolean = false, val counters: Int = 0) {
    fun isEmpty(): Boolean {
        return costs.isEmpty() && tokens.isEmpty() && !isSideways && !entersTapped && counters == 0
    }
}

fun wordToNumber(word: String?): Int? {
    return when (word) {
        "a", "an", "one" -> 1
        "two" -> 2
        "three" -> 3
        "four" -> 4
        "five" -> 5
        "six" -> 6
        "seven" -> 7
        "eight" -> 8
        "nine" -> 9
        "ten" -> 10
        else -> null
    }
}

fun parseCardExtras(card: Card, id: Id, tokenIds: Map<Id, CardDao.Printing>): CardExtra {
    val manaCosts = mutableListOf<List<String>>()
    val tokens = mutableListOf<Id>()

    if (card.cardFaces != null) {
        for(face in card.cardFaces) {
            if (face.manaCost != null) {
                manaCosts.add(parseCost(face.manaCost))
            }
            val oracleText = face.oracleText ?: ""
            manaCosts.addAll(findManaCosts(oracleText))
        }
    } else {
        if (card.manaCost != null) {
            manaCosts.add(parseCost(card.manaCost))
        }
        val oracleText = card.oracleText ?: ""
        manaCosts.addAll(findManaCosts(oracleText))
    }

    if (card.parts != null) {
        for (part in card.parts) {
            if (part.component == "token") {
                val tokenId = tokenIds[part.id]
                if (tokenId == null) {
//                    println("Token not found for $scryfallId")
                    continue
                }
                tokens.add(tokenId.cardId)
            }
        }
    }

    val text = if (card.oracleText != null) {
        card.oracleText
    } else {
        val faces = card.cardFaces!!
        faces[0].oracleText + " // " + faces[1].oracleText
    }

    // get starting counters
    val startingCounters: Int? = card.loyalty?.toIntOrNull()
        ?: card.defense?.toIntOrNull()
        ?: card.cardFaces?.getOrNull(0)?.loyalty?.toIntOrNull()
        ?: card.cardFaces?.getOrNull(0)?.defense?.toIntOrNull()
        ?: wordToNumber(startingCountersPattern.find(text)?.groupValues?.get(1))

    return CardExtra(
        id,
        manaCosts.filter { it.isNotEmpty() }.distinct(),
        tokens,
        isSideways = card.typeLine?.contains("Battle") == true,
        entersTapped = entersTappedRegex.containsMatchIn(text.replace(card.name, "~")),
        counters = startingCounters ?: 0
    )
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

internal val json = Json { ignoreUnknownKeys = true }


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
    val cards = json.decodeFromString<List<Card>>(cardsJson)
    return cards
        .filter {
            it.oracleId != null
                    && (it.imageUris != null || it.cardFaces != null)
                    && it.imageStatus != "missing"
        }
        .associate {
            val id = it.id
            val oracleId = it.oracleId!!

            val card = it
            val image = if (card.imageUris == null) {
                card.cardFaces!![0].imageUris!!.normal
            } else {
                card.imageUris.normal
            }

            val thumbnailImage = if (card.imageUris == null) {
                card.cardFaces!![0].imageUris!!.artCrop
            } else {
                card.imageUris.artCrop
            }

            val backImage = if (card.imageUris == null) {
                card.cardFaces!![1].imageUris!!.normal
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
