package starfield.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import starfield.data.dao.CardDao
import starfield.plugins.respondError
import starfield.plugins.respondSuccess
import starfield.plugins.tryParseUuid
import java.io.File

fun Route.cardRouting() {

    get("/{id}/image") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@get call.respondError("Invalid id")
        val type = call.parameters["type"]?.lowercase() ?: return@get call.respondError("Invalid image type")
        if (type !in listOf("front", "back", "thumbnail")) {
            return@get call.respondError("Invalid image type")
        }

        val storagePath = System.getenv("STORAGE_PATH") ?: "./"
        call.respondFile(File(storagePath + "cards/$uuid/$type.jpg"))
    }

    get("/{id}") {
        val uuid = tryParseUuid(call.parameters["id"]) ?: return@get call.respondError("Invalid id")

        val dao = CardDao()
        val card = dao.getCard(uuid) ?: return@get call.respondError("Card not found", HttpStatusCode.NotFound)
        call.respondSuccess(card)
    }
}