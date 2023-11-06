package dk.clausr.kanpla.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dk.clausr.kanpla.data.InstantSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.time.Instant
import java.util.concurrent.TimeUnit


object RetrofitClient {
    private const val TIME_OUT: Long = 120

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(Instant::class, InstantSerializer)
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(TIME_OUT, TimeUnit.SECONDS)
        .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
        .authenticator(KanplaAuthenticator())
        .addNetworkInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    val retrofit: KanplaApi by lazy {
        Retrofit.Builder()
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .baseUrl(ApiConstants.BaseUrl)
            .client(okHttpClient)
            .build()
            .create(KanplaApi::class.java)
    }
}
