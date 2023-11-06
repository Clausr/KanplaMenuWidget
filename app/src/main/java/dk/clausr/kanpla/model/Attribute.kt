package dk.clausr.kanpla.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Attribute(

    @SerialName("frontendDisplayMode") var frontendDisplayMode: FrontendDisplayMode,
    @SerialName("key") var key: String,
    @SerialName("names") var names: Names,
    @SerialName("type") var type: Type,
    @SerialName("name") var name: String,
) {
    @Serializable
    enum class FrontendDisplayMode {
        @SerialName("description")
        Description,

        @SerialName("tag")
        Tag,
    }

    @Serializable
    enum class Type {
        @SerialName("labels")
        Labels,

        @SerialName("allergens")
        Allergens,
    }
}
