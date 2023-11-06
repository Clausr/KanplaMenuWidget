package dk.clausr.kanpla

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dk.clausr.kanpla.data.KanplaMenuWidgetDataDefinition
import dk.clausr.kanpla.data.SerializedWidgetState
import dk.clausr.kanpla.model.MenuWidgetData
import dk.clausr.kanpla.network.RetrofitClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WidgetConfigurationViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore: DataStore<SerializedWidgetState> = runBlocking { KanplaMenuWidgetDataDefinition.getDataStore(getApplication()) }

    val data = dataStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val moduleId = dataStore.data.map { it.widgetSettings.moduleId }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val menu = moduleId
        .mapNotNull { it?.ifBlank { null } }
        .map { moduleId ->
            val dateFormatted = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(LocalDate.now())
            RetrofitClient.retrofit.getMenu(moduleId, dateFormatted).let {
                if (it.isSuccessful) {
                    it.body()?.response
                } else null
            }
        }

    fun setModuleId(moduleId: String) = viewModelScope.launch {
        dataStore.updateData { oldData ->
            SerializedWidgetState.Success(
                MenuWidgetData(
                    product = null,
                    menu = null,
                    lastUpdated = Instant.now()
                ),
                settings = oldData.widgetSettings.copy(moduleId = moduleId)
            )
        }
    }


    fun setPreferredProduct(productId: String?) = viewModelScope.launch {
        dataStore.updateData { oldData ->
            SerializedWidgetState.Success(
                MenuWidgetData(
                    product = null,
                    menu = null,
                    lastUpdated = Instant.now()
                ),
                settings = oldData.widgetSettings.copy(productId = productId)
            )
        }
    }
}
