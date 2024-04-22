package starfield.data.table

import org.jetbrains.exposed.dao.id.UUIDTable

object Users : UUIDTable("User", "user_id") {
    val name = varchar("user_name", 200).index(isUnique = true)
    val passwordHash = varchar("password_hash", 200)
    val features = integer("features").default(0)
}