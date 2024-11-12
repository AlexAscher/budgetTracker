package com.alterpat.budgettracker

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore by preferencesDataStore(name = "settings")

object DataStoreSingleton {
    fun getDataStore(context: Context) = context.dataStore
}