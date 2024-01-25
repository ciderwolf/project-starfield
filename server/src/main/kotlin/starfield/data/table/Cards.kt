package starfield.data.table

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.json


object Cards : Table("Card") {
    val id = uuid("CardId")
    val name = varchar("Name", 200)
    val fuzzyName = varchar("FuzzyName", 200).index(isUnique = true)
    val type = varchar("Type", 50)
    val src = integer("Source")
    val image = varchar("Image", 100)
    val backImage = varchar("BackImage", 100).nullable()

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