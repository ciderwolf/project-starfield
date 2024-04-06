package starfield.data.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table


object Cards : Table("Card") {

    val id = uuid("CardId")
    val name = varchar("Name", 200)
    val fuzzyName = varchar("FuzzyName", 200).index(isUnique = false)
    val type = varchar("Type", 50)
    val src = integer("Source").references(CardSources.id)
    val image = varchar("Image", 100)
    val backImage = varchar("BackImage", 100).nullable()
    val thumbnailImage = varchar("ThumbnailImage", 100)

    override val primaryKey = PrimaryKey(id)
}

object Tokens : Table("Token") {
    val id = uuid("TokenId")
    val name = varchar("Name", 100)
    val fuzzyName = varchar("FuzzyName", 100)
    val colors = varchar("Colors", 10)
    val superTypes = varchar("SuperTypes", 100)
    val subTypes = varchar("SubTypes", 100)
    val pt = varchar("PT", 10).nullable()
    val text = varchar("Text", 1000)
    val image = varchar("Image", 100)
    val backImage = varchar("BackImage", 100).nullable()

    override val primaryKey = PrimaryKey(id)
}

object CardSources : IntIdTable("CardSource", "CardSourceId") {
    val name = varchar("Name", 100)
    val code = varchar("Code", 5)
}