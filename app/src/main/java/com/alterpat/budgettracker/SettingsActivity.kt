package com.alterpat.budgettracker

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsActivity : AppCompatActivity() {

    private val THEME_KEY = stringPreferencesKey("theme")
    private val COLOR_KEY = stringPreferencesKey("color")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        applySettings()

        lifecycleScope.launch {
            val preferences = DataStoreSingleton.getDataStore(applicationContext).data.first()
            val theme = preferences[THEME_KEY] ?: "light"
            val color = preferences[COLOR_KEY] ?: "red"

            when (theme) {
                "light" -> themeGroup.check(R.id.lightTheme)
                "dark" -> themeGroup.check(R.id.darkTheme)
            }

            when (color) {
                "red" -> colorGroup.check(R.id.colorRed)
                "green" -> colorGroup.check(R.id.colorGreen)
                "blue" -> colorGroup.check(R.id.colorBlue)
                "yellow" -> colorGroup.check(R.id.colorYellow)
                "purple" -> colorGroup.check(R.id.colorPurple)
            }
        }

        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.lightTheme -> "light"
                R.id.darkTheme -> "dark"
                else -> "light"
            }
            lifecycleScope.launch {
                DataStoreSingleton.getDataStore(applicationContext).edit { preferences ->
                    preferences[THEME_KEY] = theme
                }
            }
        }

        colorGroup.setOnCheckedChangeListener { _, checkedId ->
            val color = when (checkedId) {
                R.id.colorRed -> "red"
                R.id.colorGreen -> "green"
                R.id.colorBlue -> "blue"
                R.id.colorYellow -> "yellow"
                R.id.colorPurple -> "purple"
                else -> "red"
            }
            lifecycleScope.launch {
                DataStoreSingleton.getDataStore(applicationContext).edit { preferences ->
                    preferences[COLOR_KEY] = color
                }
            }
        }
    }

    private fun applySettings() {
        runBlocking {
            val preferences = DataStoreSingleton.getDataStore(applicationContext).data.first()
            val theme = preferences[THEME_KEY] ?: "light"
            val color = preferences[COLOR_KEY] ?: "red"

            when (theme) {
                "light" -> setTheme(R.style.Theme_Light)
                "dark" -> setTheme(R.style.Theme_Dark)
            }

            val colorResId = when (color) {
                "red" -> R.color.red
                "green" -> R.color.green
                "blue" -> R.color.blue
                "yellow" -> R.color.yellow
                "purple" -> R.color.purple
                else -> R.color.red
            }

            // Примените цвет к элементам интерфейса, если необходимо
        }
    }
}