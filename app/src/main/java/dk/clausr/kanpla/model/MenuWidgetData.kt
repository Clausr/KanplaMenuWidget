package dk.clausr.kanpla.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class MenuWidgetData(
    val product: Product?,
    val menu: Menu?,
    @Contextual val lastUpdated: Instant? = null,
)

@Serializable
data class WidgetSettings(
    val moduleId: String? = null,
    val productId: String? = null,
)
