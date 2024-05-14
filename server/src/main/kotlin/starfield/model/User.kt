package starfield.model

import kotlinx.serialization.Serializable
import starfield.plugins.Id
import starfield.plugins.WSConnection
import java.util.UUID

@Serializable
data class UserState(val name: String, val id: Id)

class User(val name: String, val id: UUID, var connection: WSConnection?) {

    fun getState() = UserState(name, id)

    override fun equals(other: Any?): Boolean {
        return if (other is User) {
            other.id == id
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}