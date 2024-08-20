package starfield.plugins

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import starfield.Config
import starfield.model.User
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong


@Serializable
class UserSession(val username: String, val id: Id) {
    private fun connection() = connections.find { it.id == id }

    fun user() = User(username, id, connection())

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


}


fun Application.configureSessions() {
    install(Sessions) {
        cookie<UserSession>("user_session", directorySessionStorage(File(Config.storagePath("sessions")))) {
            cookie.extensions["SameSite"] = "lax"
            cookie.secure = true
        }
    }
}