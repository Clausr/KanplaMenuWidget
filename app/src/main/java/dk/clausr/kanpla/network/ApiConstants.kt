package dk.clausr.kanpla.network

import dk.clausr.kanpla.BuildConfig

object ApiConstants {
    const val BaseUrl = "https://api.kanpla.dk/api/v1/modules/${BuildConfig.moduleId}/"
}
