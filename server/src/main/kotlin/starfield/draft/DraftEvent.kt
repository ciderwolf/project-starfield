package starfield.draft

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import starfield.model.DraftCard
import starfield.plugins.Id

@Serializable
sealed class DraftEvent {
    @Serializable
    @SerialName("receive_pack")
    data class ReceivePackMessage(val pack: List<DraftCard>, val pickNumber: Int, val packNumber: Int) : DraftEvent()

    @Serializable
    @SerialName("pack_queue")
    data class PackQueueMessage(val packs: Map<Id, Int>) : DraftEvent()

    @Serializable
    @SerialName("end_draft")
    data class EndDraftMessage(val deckId: Id) : DraftEvent()
}