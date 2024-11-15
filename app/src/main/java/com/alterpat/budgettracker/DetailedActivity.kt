package com.alterpat.budgettracker

import android.content.Context
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

class DetailedActivity : AppCompatActivity() {
    private lateinit var transaction: Transaction
    private val THEME_KEY = stringPreferencesKey("theme")
    private lateinit var db: AppDatabase

    private lateinit var labelInput: TextInputEditText
    private lateinit var amountInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var updateBtn: Button
    private lateinit var labelLayout: TextInputLayout
    private lateinit var amountLayout: TextInputLayout
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
        updateBtn = findViewById(R.id.updateBtn)
        labelLayout = findViewById(R.id.labelLayout)
        amountLayout = findViewById(R.id.amountLayout)
        rootView = findViewById(R.id.rootView)
        closeBtn = findViewById(R.id.closeBtn)

        val header = findViewById<TextView>(R.id.header)
        header.text = "Detailed Activity"

        transaction = intent.getParcelableExtra("transaction") ?: run {
            finish()
            return
        }

        labelInput.setText(transaction.label)
        amountInput.setText(transaction.amount.toString())
        descriptionInput.setText(transaction.description)

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

        closeBtn.setOnClickListener {
            finish()
        }
    }

    private fun saveTransaction() {
        val label = labelInput.text.toString()
        val description = descriptionInput.text.toString()
        val amount = amountInput.text.toString().toDoubleOrNull()

        if(label.isEmpty())
            labelLayout.error = "Please enter a valid label"
        else if(amount == null)
            amountLayout.error = "Please enter a valid amount"
        else {
            transaction.label = label
            transaction.amount = amount
            transaction.description = description

            GlobalScope.launch {
                db.transactionDao().update(transaction)
                runOnUiThread {
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
