package dk.clausr.kanpla.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KanplaMenuResponse(
    @SerialName("status") var status: Int,
    @SerialName("reasonPhrase") var reasonPhrase: String,
    @SerialName("response") var response: Response,
)
