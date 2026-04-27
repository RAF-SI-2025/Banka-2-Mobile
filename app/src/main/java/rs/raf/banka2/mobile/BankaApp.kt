package rs.raf.banka2.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import rs.raf.banka2.mobile.BuildConfig
import timber.log.Timber

@HiltAndroidApp
class BankaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
