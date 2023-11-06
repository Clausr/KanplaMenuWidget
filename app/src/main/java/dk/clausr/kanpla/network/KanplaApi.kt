package dk.clausr.kanpla.network

import dk.clausr.kanpla.model.KanplaMenuResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KanplaApi {
    @GET("{kanplaId}/menu")
    suspend fun getMenu(@Path("kanplaId") kanplaId: String, @Query("date") date: String): Response<KanplaMenuResponse>
}
