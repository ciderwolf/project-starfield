package starfield.data.dao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import starfield.plugins.Id
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class CubeCobraDao {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class Cube(
        val name: String,
        val id: String,
        val image: CubeImage,
        val cards: CubeCards
    )

    @Serializable
    data class CubeImage(
        val uri: String
    )

    @Serializable
    data class CubeCards(
        val mainboard: List<CubeCard>
    )

    @Serializable
    data class CubeCard(
        val cardID: Id
    )

    suspend fun getCube(cubeCobraId: String): Cube? {
        val uri = "https://cubecobra.com/cube/api/cubeJSON/$cubeCobraId"
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(uri))
            .GET()
            .build()

        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        if (response.statusCode() != 200) {
            return null
        }

        val jsonText = response.body()
        return json.decodeFromString<Cube>(jsonText)
    }
}