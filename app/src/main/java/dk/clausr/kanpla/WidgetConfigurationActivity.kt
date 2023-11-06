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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import dk.clausr.kanpla.extensions.pxToDp
import dk.clausr.kanpla.ui.KanplaTheme
import dk.clausr.kanpla.worker.UpdateMenuWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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

            var moduleId: String by remember(data) { mutableStateOf(data?.widgetSettings?.moduleId ?: "") }
            val response by configurationViewModel.menu.collectAsState(initial = null)

            val keyboardController = LocalSoftwareKeyboardController.current
            val scope = rememberCoroutineScope()
            var showBottomSheet by remember { mutableStateOf(false) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)


            KanplaTheme {
                Scaffold(modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeContent,
                    topBar = {
                        TopAppBar(title = { Text(stringResource(id = R.string.app_name)) })
                    }) { contentPadding ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                            .background(MaterialTheme.colorScheme.background)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        TextField(
                            modifier = Modifier
                                .fillMaxWidth(),
                            label = { Text("ModuleId") },
                            singleLine = true,
                            value = moduleId,
                            onValueChange = { moduleId = it },
                            maxLines = 1,
                            keyboardActions = KeyboardActions(onDone = {
                                scope.launch {
                                    keyboardController?.hide()
                                }
                                configurationViewModel.setModuleId(moduleId)
                            }),
                            trailingIcon = {
                                TextButton(
                                    enabled = moduleId != data?.widgetSettings?.moduleId,
                                    onClick = {
                                        scope.launch {
                                            keyboardController?.hide()
                                        }
                                        configurationViewModel.setModuleId(moduleId)
                                    },
                                ) {
                                    Text("Approve")
                                }
                            }
                        )


                        response?.products?.let { products ->
                            val selectedProduct = products.firstOrNull { it.id == data?.widgetSettings?.productId }

                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(25))
                                .clickable { showBottomSheet = true }
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(12.dp)

                            ) {
                                Text(selectedProduct?.name ?: "Choose a product", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            data?.widgetSettings?.productId?.let { productId ->

                                val todaysMenu = response?.menus?.firstOrNull { it.productId == productId }
                                todaysMenu?.let {
                                    Column {
                                        Text("Todays menu:", style = MaterialTheme.typography.labelLarge)
                                        Text(text = it.name)
                                        Text(text = it.description)
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    Button(onClick = {
                                        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

                                        setResult(Activity.RESULT_OK, resultValue)

                                        UpdateMenuWorker.start(this@WidgetConfigurationActivity, kanplaId = moduleId, productId = productId)
                                        finish()
                                    }) {
                                        Text("Apply configuration")
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
                                    RadioRow(text = product.name, selected = product.id == data?.widgetSettings?.productId) {
                                        configurationViewModel.setPreferredProduct(productId = product.id)
                                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                                            if (!sheetState.isVisible) {
                                                showBottomSheet = false
                                            }
                                        }
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
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null
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


