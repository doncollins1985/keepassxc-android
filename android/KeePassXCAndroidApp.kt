package org.keepassxc.android

import android.app.Application
import org.keepassxc.android.biometrics.BiometricHelper

class KeePassXCAndroidApp : Application() {
    val appContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    val settingsManager = SettingsManager(application)
    val biometricHelper = BiometricHelper()
}