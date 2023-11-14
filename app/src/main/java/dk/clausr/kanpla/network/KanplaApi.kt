package dk.clausr.kanpla.network

import dk.clausr.kanpla.model.KanplaMenuResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface KanplaApi {
    @GET("menu")
    suspend fun getMenu(@Query("date") date: String): Response<KanplaMenuResponse>
}
