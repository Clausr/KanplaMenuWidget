package dk.clausr.kanpla.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Menu(
    @SerialName("name") var name: String,
    @SerialName("description") var description: String,
    @SerialName("productName") var productName: String,
    @SerialName("attributes") var attributes: List<Attribute>,
    @Contextual @SerialName("date") var date: Instant,
    @SerialName("productId") var productId: String
)
