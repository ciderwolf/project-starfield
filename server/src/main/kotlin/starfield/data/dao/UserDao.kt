package starfield.data.dao

import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import starfield.data.table.Users
import java.util.UUID

class UserDao {

    class User(val userName: String, val passwordHash: String, val id: UUID)

    private fun mapUser(row: ResultRow): User {
        return User (
            id = row[Users.id].value,
            userName = row[Users.name],
            passwordHash = row[Users.passwordHash]
        )
    }

    suspend fun login(username: String, password: String) = DatabaseSingleton.dbQuery {
        val user = Users.selectAll().where { Users.name eq username }.singleOrNull()?.let(::mapUser)
        if (user != null) {
            val passwordHash = user.passwordHash.toCharArray()
            val result = BCrypt.verifyer().verify(password.toCharArray(), passwordHash)
            if (result.verified) {
                return@dbQuery user.id
            } else {
                return@dbQuery null
            }
        }
        return@dbQuery null
    }

    private suspend fun getUser(username: String) = DatabaseSingleton.dbQuery {
        Users.selectAll().where { Users.name eq username }.singleOrNull()?.let(::mapUser)
    }

    suspend fun registerUser(username: String, password: String): UUID? {
        val preexisting = getUser(username)
        if (preexisting != null) {
            return null
        }
        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val userId = UUID.randomUUID()
        DatabaseSingleton.dbQuery {
            Users.insert {
                it[id] = userId
                it[name] = username
                it[passwordHash] = hash
            }
        }
        return userId
    }
}