package starfield.data.table

import org.jetbrains.exposed.dao.id.UUIDTable

object Users : UUIDTable("User", "UserId") {
    val name = varchar("UserName", 200).index(isUnique = true)
    val passwordHash = varchar("PasswordHash", 200)
}