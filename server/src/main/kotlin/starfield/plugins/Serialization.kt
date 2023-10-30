package starfield.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import starfield.data.CardDatabase
import starfield.model.Pivot
import starfield.model.toEnum
import java.lang.IllegalArgumentException
import java.util.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/cards/download") {
            CardDatabase.download()
            call.respond(CardDatabase.instance().size)
        }
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