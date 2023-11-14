package dk.clausr.kanpla.data

import dk.clausr.kanpla.model.MenuWidgetDataList
import dk.clausr.kanpla.model.WidgetSettings
import kotlinx.serialization.Serializable

@Serializable
sealed class SerializedWidgetState(val widgetSettings: WidgetSettings) {
    @Serializable
    data class Loading(private val settings: WidgetSettings) : SerializedWidgetState(settings)

    @Serializable
    data class Success(val data: MenuWidgetDataList, private val settings: WidgetSettings) : SerializedWidgetState(settings)

    @Serializable
    data class Error(val message: String, private val settings: WidgetSettings) : SerializedWidgetState(settings)
}
