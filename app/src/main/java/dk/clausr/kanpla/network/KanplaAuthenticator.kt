package dk.clausr.kanpla.network

import dk.clausr.kanpla.BuildConfig
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class KanplaAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request = response.request.newBuilder()
        .addHeader("Authorization", "Bearer ${BuildConfig.KANPLA_BEARER_TOKEN}")
        .build()
}
