package starfield.routing

import io.ktor.server.routing.*
import starfield.data.dao.DraftSetDao
import starfield.data.dao.MtgJsonDao
import starfield.plugins.respondError
import starfield.plugins.respondSuccess


fun Route.draftRouting() {
    get("/sets") {
        val draftSetDao = DraftSetDao()
        val sets = draftSetDao.getDraftSetListings()
        call.respondSuccess(sets)
    }

    post("/sets/{code}") {
        val code = call.parameters["code"] ?: return@post call.respondError("Code is required")
        val mtgJsonDao = MtgJsonDao()
        val boosterConfig = try {
            mtgJsonDao.downloadBoosterInfo(code)
        } catch (e: Exception) {
            return@post call.respondError("Failed to download booster info for code $code: ${e.message}")
        }

        val draftSetDao = DraftSetDao()
        val set = draftSetDao.createDraftSetFromScryfall(boosterConfig.name!!.replace(" Draft Booster", ""), code, boosterConfig)
        call.respondSuccess(set)
    }
}