package dk.clausr.kanpla.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class MenuWidgetData(
    val product: Product,
    val menu: Menu,
)

@Serializable
data class MenuWidgetDataList(
    val dailyData: List<MenuWidgetData>,
    @Contextual val lastUpdated: Instant? = null,
)

@Serializable
data class WidgetSettings(
    val productIds: List<String>? = null,
)
