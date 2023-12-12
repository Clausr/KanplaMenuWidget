package dk.clausr.kanpla

import android.app.Application
import androidx.work.Configuration

class KanplaApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration = Configuration.Builder()
        .setMinimumLoggingLevel(android.util.Log.DEBUG)
        .build()

}
