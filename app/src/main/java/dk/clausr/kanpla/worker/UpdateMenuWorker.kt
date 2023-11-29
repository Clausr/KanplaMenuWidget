package dk.clausr.kanpla.worker

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dk.clausr.kanpla.data.KanplaMenuWidgetDataDefinition
import dk.clausr.kanpla.data.SerializedWidgetState
import dk.clausr.kanpla.model.MenuWidgetData
import dk.clausr.kanpla.model.MenuWidgetDataList
import dk.clausr.kanpla.network.RetrofitClient
import dk.clausr.kanpla.utils.getDesiredDate
import dk.clausr.kanpla.widget.MenuOfTheDayWidget
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class UpdateMenuWorker(
    private val appContext: Context, private val workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        lateinit var workerResult: Result

        val dataStore = KanplaMenuWidgetDataDefinition.getDataStore(appContext)

        var splitTime: LocalTime = LocalTime.NOON
        dataStore.updateData { oldState ->
            splitTime = oldState.widgetSettings.tomorrowDataLookupTime
            SerializedWidgetState.Loading(oldState.widgetSettings)
        }

        MenuOfTheDayWidget.updateAll(appContext)

        val todayOrNextWeekdayDate = getDesiredDate(splitTime)

        val productIds = workerParameters.inputData.getStringArray(ProductIdKeys)?.toList() ?: return Result.failure()
        Log.d("Worker", "productIds: ${productIds.joinToString { it }}")

        val dateFormatted = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(todayOrNextWeekdayDate)
        val result = RetrofitClient.retrofit.getMenu(dateFormatted)

        when {
            result.isSuccessful && result.body() != null -> {
                val response = result.body()!!.response

                val dailyData = response.products.filter { it.id in productIds }.map { product ->
                    MenuWidgetData(product, response.menus.first { it.productId == product.id })
                }

                dataStore.updateData { oldState ->
                    SerializedWidgetState.Success(
                        data = MenuWidgetDataList(
                            dailyData = dailyData,
                            lastUpdated = Instant.now(),
                        ),
                        settings = oldState.widgetSettings
                    )
                }
                workerResult = Result.success()
            }

            else -> {
                dataStore.updateData { oldData ->
                    SerializedWidgetState.Error(result.message(), oldData.widgetSettings)
                }
                workerResult = Result.failure()
            }
        }
        MenuOfTheDayWidget.updateAll(appContext)
        return workerResult
    }

    companion object {
        const val ProductIdKeys = "productIdKeys"
        private const val updateMenuWorkerUniqueName = "UpdateMenuWorkerUniqueName"

        fun startSingle(productIds: List<String>) =
            OneTimeWorkRequestBuilder<UpdateMenuWorker>().setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
            .setInputData(
                workDataOf(
                    ProductIdKeys to productIds.toTypedArray()
                )
            )
            .build()

        fun enqueueUnique(context: Context, productIds: List<String>) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                updateMenuWorkerUniqueName, ExistingWorkPolicy.KEEP, startSingle(productIds)
            )
        }

        private const val periodicSync = "PeriodicSyncWorker"

        private val periodicConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        private fun periodicWorkSync(productId: List<String>) =
            PeriodicWorkRequestBuilder<UpdateMenuWorker>(repeatInterval = Duration.ofHours(2))
                .setInputData(workDataOf(ProductIdKeys to productId.toTypedArray()))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(10))
                .setInitialDelay(Duration.ofSeconds(10))
                .setConstraints(periodicConstraints)
                .build()

        fun start(
            context: Context,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
            productId: List<String>
        ) {
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    periodicSync,
                    policy,
                    periodicWorkSync(productId)
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(periodicSync)
        }
    }
}
