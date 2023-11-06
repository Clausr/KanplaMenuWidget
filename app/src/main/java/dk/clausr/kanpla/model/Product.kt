package dk.clausr.kanpla.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    @SerialName("name") var name: String,
    @SerialName("id") var id: String,
    @SerialName("category") var category: String,
    @SerialName("order") var order: Int,
)
