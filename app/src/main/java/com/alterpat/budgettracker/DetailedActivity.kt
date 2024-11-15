package com.alterpat.budgettracker

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.core.widget.addTextChangedListener
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailedActivity : AppCompatActivity() {
    private lateinit var transaction: Transaction
    private val THEME_KEY = stringPreferencesKey("theme")
    private lateinit var db: AppDatabase

    private lateinit var labelInput: TextInputEditText
    private lateinit var amountInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var dateInput: TextInputEditText
    private lateinit var updateBtn: Button
    private lateinit var labelLayout: TextInputLayout
    private lateinit var amountLayout: TextInputLayout
    private lateinit var dateLayout: TextInputLayout
    private lateinit var rootView: View
    private lateinit var closeBtn: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applySettings()
        setContentView(R.layout.activity_detailed)

        db = Room.databaseBuilder(this, AppDatabase::class.java, "transactions").build()

        labelInput = findViewById(R.id.labelInput)
        amountInput = findViewById(R.id.amountInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        dateInput = findViewById(R.id.dateInput)
        updateBtn = findViewById(R.id.updateBtn)
        labelLayout = findViewById(R.id.labelLayout)
        amountLayout = findViewById(R.id.amountLayout)
        dateLayout = findViewById(R.id.dateLayout)
        rootView = findViewById(R.id.rootView)
        closeBtn = findViewById(R.id.closeBtn)

        val header = findViewById<TextView>(R.id.header)
        header.text = "Detailed Activity"

        transaction = intent.getParcelableExtra("transaction") ?: run {
            finish()
            return
        }

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        labelInput.setText(transaction.label)
        amountInput.setText(transaction.amount.toString())
        descriptionInput.setText(transaction.description)
        dateInput.setText(dateFormat.format(Date(transaction.date)))

        updateBtn.setOnClickListener {
            saveTransaction()
        }

        rootView.setOnClickListener {
            this.window.decorView.clearFocus()

            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

        labelInput.addTextChangedListener {
            updateBtn.visibility = View.VISIBLE
            if(it!!.count() > 0)
                labelLayout.error = null
        }

        amountInput.addTextChangedListener {
            updateBtn.visibility = View.VISIBLE
            if(it!!.count() > 0)
                amountLayout.error = null
        }

        descriptionInput.addTextChangedListener {
            updateBtn.visibility = View.VISIBLE
        }

        dateInput.addTextChangedListener {
            updateBtn.visibility = View.VISIBLE
            if(it!!.count() > 0)
                dateLayout.error = null
        }

        closeBtn.setOnClickListener {
            finish()
        }
    }

    private fun saveTransaction() {
        val label = labelInput.text.toString()
        val description = descriptionInput.text.toString()
        val amount = amountInput.text.toString().toDoubleOrNull()
        val dateString = dateInput.text.toString()
        val date = try {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            null
        }

        if(label.isEmpty())
            labelLayout.error = "Please enter a valid label"
        else if(amount == null)
            amountLayout.error = "Please enter a valid amount"
        else if(date == null)
            dateLayout.error = "Please enter a valid date"
        else {
            transaction.label = label
            transaction.amount = amount
            transaction.description = description
            transaction.date = date

            GlobalScope.launch {
                db.transactionDao().update(transaction)
                runOnUiThread {
                    val intent = Intent(this@DetailedActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
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
