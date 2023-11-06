package dk.clausr.kanpla.data

import dk.clausr.kanpla.model.MenuWidgetData
import dk.clausr.kanpla.model.WidgetSettings
import kotlinx.serialization.Serializable

@Serializable
sealed class SerializedWidgetState(val widgetSettings: WidgetSettings) {
    @Serializable
    data class Loading(val settings: WidgetSettings) : SerializedWidgetState(settings)

    @Serializable
    data class Success(val data: MenuWidgetData, val settings: WidgetSettings) : SerializedWidgetState(settings)

    @Serializable
    data class Error(val message: String, val settings: WidgetSettings) : SerializedWidgetState(settings)
}
