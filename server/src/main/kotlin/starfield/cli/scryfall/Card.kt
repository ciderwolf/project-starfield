package starfield.cli.scryfall

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import starfield.plugins.Id

@Serializable
data class Card(
    val name: String,
    val id: Id,
    @SerialName("set_type") val setType: String,
    @SerialName("type_line") val typeLine: String? = null,
    @SerialName("oracle_id") val oracleId: Id? = null,
    @SerialName("power") val power: String? = null,
    @SerialName("toughness") val toughness: String? = null,
    @SerialName("loyalty") val loyalty: String? = null,
    @SerialName("defense") val defense: String? = null,
    @SerialName("oracle_text") val oracleText: String? = null,
    @SerialName("card_faces") val cardFaces: List<CardFace>? = null,
    @SerialName("image_uris") val imageUris: ImageUris? = null,
    @SerialName("image_status") val imageStatus: String? = null,
    @SerialName("color_identity") val colorIdentity: List<String>? = null,
    @SerialName("mana_cost") val manaCost: String? = null,
    @SerialName("all_parts") val parts: List<CardPart>? = null
)

@Serializable
data class CardFace(
    val name: String,
    @SerialName("type_line") val typeLine: String? = null,
    @SerialName("oracle_text") val oracleText: String? = null,
    @SerialName("power") val power: String? = null,
    @SerialName("toughness") val toughness: String? = null,
    @SerialName("loyalty") val loyalty: String? = null,
    @SerialName("defense") val defense: String? = null,
    @SerialName("image_uris") val imageUris: ImageUris? = null,
    @SerialName("mana_cost") val manaCost: String? = null,
)

@Serializable
data class ImageUris(
    @SerialName("normal") val normal: String,
    @SerialName("large") val large: String,
    @SerialName("png") val png: String,
    @SerialName("art_crop") val artCrop: String,
    @SerialName("border_crop") val borderCrop: String,
)

@Serializable
data class CardPart(
    val component: String,
    val id: Id
)