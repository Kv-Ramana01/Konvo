package com.example.konvo

import android.app.Application
import com.example.konvo.BuildConfig
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KonvoApp : Application() {

    override fun onCreate() {
        super.onCreate()


        Firebase.initialize(this)


        val appCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {

            Log.d("APP_CHECK", "Installing Debug provider")
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {

            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
