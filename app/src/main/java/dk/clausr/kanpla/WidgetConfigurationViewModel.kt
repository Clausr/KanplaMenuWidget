package dk.clausr.kanpla

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dk.clausr.kanpla.data.KanplaMenuWidgetDataDefinition
import dk.clausr.kanpla.data.SerializedWidgetState
import dk.clausr.kanpla.network.RetrofitClient
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WidgetConfigurationViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore: DataStore<SerializedWidgetState> = runBlocking { KanplaMenuWidgetDataDefinition.getDataStore(getApplication()) }

    val data = dataStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val menu = flow {
        val dateFormatted = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(LocalDate.now())
        val result = RetrofitClient.retrofit.getMenu(dateFormatted).let {
            if (it.isSuccessful) {
                it.body()?.response
            } else null
        }
        emit(result)
    }

    fun setPreferredProduct(productId: String) = viewModelScope.launch {
        dataStore.updateData { oldData ->
            val chosenProductIds = oldData.widgetSettings.productIds?.toMutableSet() ?: mutableSetOf()
            if (chosenProductIds.contains(productId)) {
                chosenProductIds.remove(productId)
            } else {
                chosenProductIds.add(productId)
            }

            SerializedWidgetState.Loading(
                settings = oldData.widgetSettings.copy(productIds = chosenProductIds.toList())
            )
        }
    }
}
