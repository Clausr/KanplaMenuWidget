package dk.clausr.kanpla

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import dk.clausr.kanpla.extensions.productNameWithEmojis
import dk.clausr.kanpla.ui.KanplaTheme
import dk.clausr.kanpla.widget.getDayOfWeekString
import dk.clausr.kanpla.worker.UpdateMenuWorker

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
class WidgetConfigurationActivity : ComponentActivity() {

    private val appWidgetId: Int by lazy {
        intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private val configurationViewModel: WidgetConfigurationViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application).create(WidgetConfigurationViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setResult(Activity.RESULT_CANCELED)

        updateView()

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

    private fun updateView() {
        setContent {
            val data by configurationViewModel.data.collectAsState()
            val response by configurationViewModel.menu.collectAsState(initial = null)

            var chosenProductIds: List<String> by remember(data?.widgetSettings?.productIds) {
                mutableStateOf(
                    data?.widgetSettings?.productIds ?: emptyList()
                )
            }
            KanplaTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                    TopAppBar(title = { Text(stringResource(id = R.string.app_name)) })
                }) { contentPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .padding(horizontal = 16.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

//                        Text("Time: ${data?.widgetSettings?.tomorrowDataLookupTime}", modifier = Modifier.clickable {
//                            configurationViewModel.setTime()
//                        })

                        response?.products?.let { products ->
                            val selectedProducts = products.filter { data?.widgetSettings?.productIds?.contains(it.id) == true }

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            ) {
                                products.forEach { product ->
                                    FilterChip(
                                        selected = product in selectedProducts,
                                        onClick = {
                                            if (chosenProductIds.contains(product.id)) {
                                                chosenProductIds -= product.id
                                            } else {
                                                chosenProductIds += product.id
                                            }
                                            configurationViewModel.setPreferredProduct(productId = product.id)
                                        },
                                        label = { Text(product.productNameWithEmojis) },
                                    )
                                }
                            }

                            data?.widgetSettings?.productIds?.let { productIds ->
                                val todaysMenu = response?.menus?.filter { productIds.contains(it.productId) }

                                Text(modifier = Modifier.padding(top = 16.dp),
                                    text = "Menu for ${todaysMenu?.firstOrNull()?.date?.getDayOfWeekString()?.replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                                todaysMenu?.forEach {
                                    Column {
                                        Text(
                                            text = it.productNameWithEmojis, style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = it.name.ifBlank { it.productName }, style = MaterialTheme.typography.bodySmall
                                        )

                                        Text(
                                            text = it.description, style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
                                ) {
                                    Button(onClick = {
                                        val resultValue = Intent().putExtra(
                                            AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId
                                        )

                                        setResult(Activity.RESULT_OK, resultValue)

                                        UpdateMenuWorker.start(
                                            this@WidgetConfigurationActivity, productId = chosenProductIds.toList()
                                        )
                                        finish()
                                    }) {
                                        Text("Apply configuration")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
