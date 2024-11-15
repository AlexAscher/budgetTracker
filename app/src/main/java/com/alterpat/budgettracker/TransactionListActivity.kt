package com.alterpat.budgettracker

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import android.widget.ImageButton
import android.widget.TextView

class TransactionListActivity : AppCompatActivity() {
    private lateinit var transactions: List<Transaction>
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var db: AppDatabase

    private val THEME_KEY = stringPreferencesKey("theme")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_list)
        applySettings()

        transactions = arrayListOf()
        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(this)

        db = Room.databaseBuilder(this, AppDatabase::class.java, "transactions").build()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.apply {
            adapter = transactionAdapter
            layoutManager = linearLayoutManager
        }

        val header = findViewById<TextView>(R.id.header)
        val closeBtn = findViewById<ImageButton>(R.id.closeBtn)

        val type = intent.getStringExtra("type")
        header.text = when (type) {
            "budget" -> "Budget Transactions"
            "expense" -> "Expense Transactions"
            else -> "Transactions"
        }

        closeBtn.setOnClickListener {
            finish()
        }

        fetchTransactions(type)
    }

    private fun fetchTransactions(type: String?) {
        GlobalScope.launch {
            transactions = when (type) {
                "budget" -> db.transactionDao().getPositiveTransactions()
                "expense" -> db.transactionDao().getNegativeTransactions()
                else -> listOf()
            }

            runOnUiThread {
                transactionAdapter.setData(transactions)
            }
        }
    }

    private fun applySettings() {
        runBlocking {
            val preferences = DataStoreSingleton.getDataStore(applicationContext).data.first()
            val theme = preferences[THEME_KEY] ?: "light"

            when (theme) {
                "light" -> {
                    setTheme(R.style.Theme_Light)
                    findViewById<View>(R.id.rootView).setBackgroundColor(ContextCompat.getColor(this@TransactionListActivity, R.color.white))
                }
                "dark" -> {
                    setTheme(R.style.Theme_Dark)
                    findViewById<View>(R.id.rootView).setBackgroundColor(ContextCompat.getColor(this@TransactionListActivity, R.color.gray))
                }
            }
        }
    }
}