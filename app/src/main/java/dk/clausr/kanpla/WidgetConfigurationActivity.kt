package dk.clausr.kanpla

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import dk.clausr.kanpla.ui.KanplaTheme
import dk.clausr.kanpla.widget.getDayOfWeekString
import dk.clausr.kanpla.worker.UpdateMenuWorker

@OptIn(ExperimentalMaterial3Api::class)
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

            var showBottomSheet by remember { mutableStateOf(false) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            var chosenProductIds: List<String> by remember(data?.widgetSettings?.productIds) { mutableStateOf(data?.widgetSettings?.productIds ?: emptyList()) }
            KanplaTheme {
                Scaffold(modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = { Text(stringResource(id = R.string.app_name)) })
                    }) { contentPadding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .padding(horizontal = 16.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        response?.products?.let { products ->
                            val selectedProducts = products.filter { data?.widgetSettings?.productIds?.contains(it.id) == true }

                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(25))
                                .clickable { showBottomSheet = true }
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(12.dp)

                            ) {
                                Text(
                                    selectedProducts.joinToString { it.name }.ifBlank { "Choose products" },
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (data?.widgetSettings?.productIds?.isNotEmpty() == true) {

                                data?.widgetSettings?.productIds?.let { productIds ->
                                    val todaysMenu = response?.menus?.filter { productIds.contains(it.productId) }

                                    Text(
                                        modifier = Modifier.padding(top = 16.dp),
                                        text = "Menu for ${todaysMenu?.firstOrNull()?.date?.getDayOfWeekString()?.replaceFirstChar { it.uppercase() }}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )

                                    todaysMenu?.forEach {
                                        Column {
                                            Text(text = it.name.ifBlank { it.productName }, style = MaterialTheme.typography.labelLarge)
                                            Text(text = it.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                        Button(onClick = {
                                            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

                                            setResult(Activity.RESULT_OK, resultValue)

                                            UpdateMenuWorker.start(this@WidgetConfigurationActivity, productId = chosenProductIds.toList())
                                            finish()
                                        }) {
                                            Text("Apply configuration")
                                        }
                                    }

                                }
                            }
                        }
                    }

                    if (showBottomSheet) {
                        val products = response?.products ?: emptyList()

                        KanplaBottomSheet(
                            sheetState = sheetState,
                            onDismissRequest = { showBottomSheet = false },
                            items = products.map { product ->
                                {
                                    RadioRow(text = product.name, selected = chosenProductIds.contains(product.id)) {
                                        if (chosenProductIds.contains(product.id)) chosenProductIds -= product.id else chosenProductIds += product.id
                                        configurationViewModel.setPreferredProduct(productId = product.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RadioRow(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.Switch,
                onClick = onSelect,
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = selected,
            onCheckedChange = null
        )
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanplaBottomSheet(
    onDismissRequest: () -> Unit,
    items: List<@Composable () -> Unit>,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        items.forEach { it() }
    }
}


