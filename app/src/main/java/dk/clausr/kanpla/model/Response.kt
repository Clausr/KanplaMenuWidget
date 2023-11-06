package dk.clausr.kanpla.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Response(
    @SerialName("products") var products: List<Product>,
    @SerialName("menus") var menus: List<Menu>,
)
