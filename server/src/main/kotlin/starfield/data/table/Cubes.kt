package starfield.data.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table

object Cubes : UUIDTable("cube") {
    val ownerId = uuid("owner_id").references(Users.id)
    val name = text("name")
    val thumbnailImage = text("thumbnail_image")
    val src = byte("source")
    val remoteId = text("remote_id").nullable()
}

object CubeCards : Table("cubecard") {
    val cubeId = uuid("cube_id").references(Cubes.id)
    val printingId = uuid("printing_id").references(Printings.id)
    val count = integer("count")
}