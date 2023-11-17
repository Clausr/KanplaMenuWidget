package dk.clausr.kanpla.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dk.clausr.kanpla.R
import dk.clausr.kanpla.data.KanplaMenuWidgetDataDefinition
import dk.clausr.kanpla.data.SerializedWidgetState
import dk.clausr.kanpla.worker.UpdateMenuWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

object MenuOfTheDayWidget : GlanceAppWidget() {
    override var stateDefinition: GlanceStateDefinition<SerializedWidgetState> = KanplaMenuWidgetDataDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val state = currentState<SerializedWidgetState>()
        val context = LocalContext.current

        Box(
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background.getColor(context))
                .appWidgetBackground()
        ) {
            when (state) {
                is SerializedWidgetState.Error -> ErrorState(state = state)
                is SerializedWidgetState.Loading -> LoadingState()
                is SerializedWidgetState.Success -> SuccessState(state = state)
            }
        }
    }

    @Composable
    private fun SuccessState(state: SerializedWidgetState.Success) {
        Box {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(state.data.dailyData) { index, it ->
                    Column(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text(
                            modifier = GlanceModifier.fillMaxWidth(),
                            text = it.menu.name.ifBlank { it.menu.productName },
                            style = TextStyle(
                                color = GlanceTheme.colors.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            modifier = GlanceModifier.fillMaxWidth()
                                .padding(bottom = if (index != state.data.dailyData.size - 1) 8.dp else 0.dp),
                            text = it.menu.description,
                            style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = TextUnit(12f, TextUnitType.Sp))
                        )
                    }
                }
            }
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dateForMenu = state.data.dailyData.first().menu.date

            Spacer(GlanceModifier.defaultWeight())

            Text(
                modifier = GlanceModifier.padding(end = 2.dp),
                text = dateForMenu.getDayOfWeekString().replaceFirstChar { it.uppercase() },
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End
                )
            )

            Image(
                modifier = GlanceModifier.wrapContentHeight().padding(2.dp)
                    .clickable(actionRunCallback<RefreshAction>()),
                provider = ImageProvider(R.drawable.refresh_24px),
                contentDescription = "Refresh",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground)
            )
        }
    }

    @Composable
    private fun ErrorState(state: SerializedWidgetState.Error) {
        Text(text = state.message)
    }

    @Composable
    private fun LoadingState() {
        Box(GlanceModifier.fillMaxSize().clickable(actionRunCallback<RefreshAction>()), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

fun Instant.getDayOfWeekString(): String {
    val zonedDateTime = this.atZone(ZoneId.systemDefault())
    val dayOfWeek = zonedDateTime.dayOfWeek
    return dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        var chosenProductIds: List<String>? = null

        updateAppWidgetState(context, KanplaMenuWidgetDataDefinition, glanceId) { oldState ->
            chosenProductIds = oldState.widgetSettings.productIds

            SerializedWidgetState.Loading(oldState.widgetSettings)
        }

        MenuOfTheDayWidget.update(context, glanceId)

        if (chosenProductIds != null) {
            UpdateMenuWorker.enqueueUnique(context, chosenProductIds ?: emptyList())
        }
    }
}

class MenuOfTheDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = MenuOfTheDayWidget

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        val widgetState = runBlocking(Dispatchers.IO) { KanplaMenuWidgetDataDefinition.getDataStore(context).data.first() }
        val productId = widgetState.widgetSettings.productIds ?: return

        UpdateMenuWorker.start(context, productId = productId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        UpdateMenuWorker.cancel(context)
    }
}
