package starfield.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import starfield.data.dao.CardDao
import starfield.engine.Pivot
import java.lang.IllegalArgumentException
import java.util.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
}

object PivotSerializer : KSerializer<Pivot> {
    override val descriptor = PrimitiveSerialDescriptor("Pivot", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Pivot {
        return decoder.decodeInt().toEnum<Pivot>()!!
    }

    override fun serialize(encoder: Encoder, value: Pivot) {
        encoder.encodeInt(value.ordinal)
    }
}

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
}

fun tryParseUuid(value: String?): UUID? {
    if (value == null) {
        return null
    }

    return try {
        UUID.fromString(value)
    } catch (e: IllegalArgumentException) {
        null
    }
}
typealias Id = @Serializable(with= UUIDSerializer::class) UUID

inline fun <reified T : Enum<T>> Int.toEnum(): T? {
    return enumValues<T>().firstOrNull { it.ordinal == this }
}

inline fun <reified T : Enum<T>> Byte.toEnum(): T? {
    return this.toInt().toEnum<T>()
}