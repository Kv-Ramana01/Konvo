package com.example.konvo.util

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.themeDataStore by preferencesDataStore(name = "theme_prefs") 