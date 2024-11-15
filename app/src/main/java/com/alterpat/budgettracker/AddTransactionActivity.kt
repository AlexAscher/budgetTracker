package com.alterpat.budgettracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_add_transaction.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {
    private val THEME_KEY = stringPreferencesKey("theme")
    private lateinit var db: AppDatabase
    private lateinit var dateInput: TextInputEditText
    private lateinit var dateLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applySettings()
        setContentView(R.layout.activity_add_transaction)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "transactions"
        ).build()

        dateInput = findViewById(R.id.dateInput)
        dateLayout = findViewById(R.id.dateLayout)

        labelInput.addTextChangedListener {
            if(it!!.count() > 0)
                labelLayout.error = null
        }

        amountInput.addTextChangedListener {
            if(it!!.count() > 0)
                amountLayout.error = null
        }

        addTransactionBtn.setOnClickListener {
            val label = labelInput.text.toString()
            val description = descriptionInput.text.toString()
            val amount = amountInput.text.toString().toDoubleOrNull()
            val dateString = dateInput.text.toString()
            val date = try {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateString)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            if(label.isEmpty())
                labelLayout.error = "Please enter a valid label"
            else if(amount == null)
                amountLayout.error = "Please enter a valid amount"
            else {
                val transaction = Transaction(0, label, amount, description, date)
                saveTransaction(transaction)
            }
        }

        closeBtn.setOnClickListener {
            finish()
        }
    }

    private fun saveTransaction(transaction: Transaction) {
        GlobalScope.launch {
            db.transactionDao().insertAll(transaction)
            finish()
        }
    }

    private fun applySettings() {
        runBlocking {
            val preferences = DataStoreSingleton.getDataStore(applicationContext).data.first()
            val theme = preferences[THEME_KEY] ?: "light"

            when (theme) {
                "light" -> setTheme(R.style.Theme_Light)
                "dark" -> setTheme(R.style.Theme_Dark)
            }
        }
    }
}