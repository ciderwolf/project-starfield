package starfield.routing

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import starfield.data.dao.CardDao
import starfield.plugins.respondSuccess

fun Route.setRouting() {
    get {
        val cardDao = CardDao()
        val sources = cardDao.getSources()
        return@get call.respondSuccess(sources)
    }
}