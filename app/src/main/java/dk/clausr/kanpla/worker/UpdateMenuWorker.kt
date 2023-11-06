package dk.clausr.kanpla.worker

import android.content.Context
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
import dk.clausr.kanpla.data.SerializedWidgetState
import dk.clausr.kanpla.model.MenuWidgetData
import dk.clausr.kanpla.network.RetrofitClient
import dk.clausr.kanpla.data.KanplaMenuWidgetDataDefinition
import dk.clausr.kanpla.widget.MenuOfTheDayWidget
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UpdateMenuWorker(private val appContext: Context, private val workerParameters: WorkerParameters) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        lateinit var workerResult: Result

        val dataStore = KanplaMenuWidgetDataDefinition.getDataStore(appContext)

        dataStore.updateData { oldState ->
            SerializedWidgetState.Loading(oldState.widgetSettings)
        }

        MenuOfTheDayWidget.updateAll(appContext)

        val todayOrNextWeekdayDate = getWeekdayDate()

        val kanplaId = workerParameters.inputData.getString(KanplaIdKey) ?: return Result.failure()
        val productId = workerParameters.inputData.getString(ProductIdKey) ?: return Result.failure()
        val dateFormatted = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(todayOrNextWeekdayDate)
        val result = RetrofitClient.retrofit.getMenu(kanplaId, dateFormatted)

        when {
            result.isSuccessful && result.body() != null -> {
                val response = result.body()!!.response
                dataStore.updateData { oldState ->
                    SerializedWidgetState.Success(
                        MenuWidgetData(
                            product = response.products.first { it.id == productId },
                            menu = response.menus.first { it.productId == productId },
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

    private fun getWeekdayDate(): LocalDate {
        var date = LocalDate.now()

        // Check if it's a weekend, and if so, move to the next weekday
        while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            date = date.plusDays(1)
        }

        return date
    }

    companion object {
        const val KanplaIdKey = "kanplaIdKey"
        const val ProductIdKey = "productIdKey"
        private const val updateMenuWorkerUniqueName = "UpdateMenuWorkerUniqueName"

        fun startSingle(kanplaId: String, productId: String) = OneTimeWorkRequestBuilder<UpdateMenuWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(
                workDataOf(
                    KanplaIdKey to kanplaId,
                    ProductIdKey to productId
                )
            )
            .build()

        fun enqueueUnique(context: Context, kanplaId: String, productId: String) {
            WorkManager.getInstance(context).enqueueUniqueWork(updateMenuWorkerUniqueName, ExistingWorkPolicy.KEEP, startSingle(kanplaId, productId))
        }

        private const val periodicSync = "PeriodicSyncWorker"

        private val periodicConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        private fun periodicWorkSync(kanplaId: String, productId: String) =
            PeriodicWorkRequestBuilder<UpdateMenuWorker>(repeatInterval = Duration.ofHours(2))
                .setInputData(workDataOf(KanplaIdKey to kanplaId, ProductIdKey to productId))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(10))
                .setInitialDelay(Duration.ofSeconds(10))
                .setConstraints(periodicConstraints)
                .build()

        fun start(
            context: Context,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
            kanplaId: String,
            productId: String
        ) {
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    periodicSync,
                    policy,
                    periodicWorkSync(kanplaId, productId)
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(periodicSync)
        }
    }
}
