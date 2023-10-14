package starfield
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Project

@Serializable
@SerialName("owned")
data class OwnedProject(val name: String, val owner: String) : Project()

inline fun doSend(obj: Project) {
    println(Json.encodeToString(obj))
}

fun main() {
    val data = OwnedProject("kotlinx.coroutines", "kotlin")
    doSend(data)
}