package starfield.plugins

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.model.User
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong


@Serializable
class UserSession(val username: String, @Serializable(with = UUIDSerializer::class) val id: UUID) {

    fun connection() = connections.find { it.id == id }

    companion object {
        private var lastId = AtomicLong(0)

        fun createNew(username: String, userId: UUID) = UserSession(username, userId)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserSession

        if (username != other.username) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    fun user(): User {
        return User(username, id, connection())
    }
}


fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("user_session")
    }
}