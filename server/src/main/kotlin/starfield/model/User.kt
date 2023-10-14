package starfield.model

import starfield.plugins.WSConnection
import java.util.UUID

class User(val name: String, val id: UUID, var connection: WSConnection?) {

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