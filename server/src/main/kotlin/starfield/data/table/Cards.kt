package starfield.data.table

import org.jetbrains.exposed.sql.Table



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