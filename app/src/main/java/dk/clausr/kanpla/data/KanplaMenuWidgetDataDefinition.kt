package dk.clausr.kanpla.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import dk.clausr.kanpla.model.WidgetSettings
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalTime

@OptIn(ExperimentalSerializationApi::class)

object KanplaMenuWidgetDataDefinition : GlanceStateDefinition<SerializedWidgetState> {
    const val fileName = "_KANPLA_MENU_DATASTORE_FILE"

    private val Context.kanplaMenuDataStore by dataStore(fileName, WidgetStateDataSerializer)

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<SerializedWidgetState> = context.kanplaMenuDataStore
    suspend fun getDataStore(context: Context): DataStore<SerializedWidgetState> = getDataStore(context, fileName)

    override fun getLocation(context: Context, fileKey: String): File = context.dataStoreFile(fileName)

    object WidgetStateDataSerializer : Serializer<SerializedWidgetState> {

        private val kSerializable = SerializedWidgetState.serializer()

        override val defaultValue: SerializedWidgetState
            get() = SerializedWidgetState.Loading(WidgetSettings())

        private val json = Json {
            serializersModule = SerializersModule {
                contextual(Instant::class, InstantSerializer)
                contextual(LocalTime::class, LocalTimeSerializer)
            }
        }

        override suspend fun readFrom(input: InputStream): SerializedWidgetState {
            return try {
                input.use { stream ->
                    json.decodeFromStream(kSerializable, stream)
                }
            } catch (exception: SerializationException) {
                throw CorruptionException("Could not read location data: ${exception.message}")
            } catch (e: Exception) {
                throw e
            }
        }

        override suspend fun writeTo(t: SerializedWidgetState, output: OutputStream) {
            output.use { stream ->
                json.encodeToStream(kSerializable, t, stream)
            }
        }
    }
}
