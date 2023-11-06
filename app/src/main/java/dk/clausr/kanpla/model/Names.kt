package dk.clausr.kanpla.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Names(
    @SerialName("da") var da: String,
    @SerialName("en") var en: String,
    @SerialName("no") var no: String,
    @SerialName("nl") var nl: String,
)
