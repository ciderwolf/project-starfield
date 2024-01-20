package starfield.data.dao

import org.jetbrains.exposed.sql.upsert
import starfield.data.table.Users
import java.util.UUID

class UserDao {

    suspend fun login(username: String, userId: UUID) = DatabaseSingleton.dbQuery {
        Users.upsert {
            it[id] = userId
            it[name] = username
        }
    }
}