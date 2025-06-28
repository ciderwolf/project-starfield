package starfield.data.table

import org.jetbrains.exposed.dao.id.UUIDTable

object DraftSets : UUIDTable("DraftSet") {
    val name = text("name")
    val setType = integer("set_type")
    val image = text("image").nullable()
    val boosterInfo = text("booster_info")
    val strategy = text("strategy").nullable()
}