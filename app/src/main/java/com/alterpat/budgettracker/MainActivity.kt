package com.alterpat.budgettracker

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_add_transaction.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var deletedTransaction: Transaction
    private lateinit var transactions : List<Transaction>
    private lateinit var oldTransactions : List<Transaction>
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var db : AppDatabase

    private val THEME_KEY = stringPreferencesKey("theme")
    private val COLOR_KEY = stringPreferencesKey("color")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        applySettings()

        transactions = arrayListOf()

        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(this)

        db = Room.databaseBuilder(this,
            AppDatabase::class.java,
            "transactions")
            .fallbackToDestructiveMigration() // Добавьте это
            .build()

        recyclerview.apply {
            adapter = transactionAdapter
            layoutManager = linearLayoutManager
        }

        // swipe to remove
        val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                deleteTransaction(transactions[viewHolder.adapterPosition])
            }

        }

        val swipeHelper = ItemTouchHelper(itemTouchHelper)
        swipeHelper.attachToRecyclerView(recyclerview)

        addBtn.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }

        val settingsBtn: ImageButton = findViewById(R.id.settingsBtn)
        settingsBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.budget_layout).setOnClickListener {
            val intent = Intent(this, TransactionListActivity::class.java)
            intent.putExtra("type", "budget")
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.expense_layout).setOnClickListener {
            val intent = Intent(this, TransactionListActivity::class.java)
            intent.putExtra("type", "expense")
            startActivity(intent)
        }

        val spinner: Spinner = findViewById(R.id.spinner_time_period)
        ArrayAdapter.createFromResource(
            this,
            R.array.time_periods,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                fetchTransactionsByPeriod(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onResume() {
        super.onResume()
        applySettings()
        val spinner: Spinner = findViewById(R.id.spinner_time_period)
        fetchTransactionsByPeriod(spinner.selectedItemPosition)
    }

    private fun applySettings() {
        runBlocking {
            val preferences = DataStoreSingleton.getDataStore(applicationContext).data.first()
            val theme = preferences[THEME_KEY] ?: "light"

            when (theme) {
                "light" -> {
                    setTheme(R.style.Theme_Light)
                    findViewById<View>(R.id.coordinator).setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    findViewById<View>(R.id.main_layout).setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                }
                "dark" -> {
                    setTheme(R.style.Theme_Dark)
                    findViewById<View>(R.id.coordinator).setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.gray))
                    findViewById<View>(R.id.main_layout).setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.gray))
                }
            }
        }
    }

    private fun fetchAll(){
        GlobalScope.launch {
            transactions = db.transactionDao().getAll()

            runOnUiThread {
                updateDashboard()
                transactionAdapter.setData(transactions)
            }
        }
    }
    private fun updateDashboard(){
        val totalAmount = transactions.map { it.amount }.sum()
        val budgetAmount = transactions.filter { it.amount>0 }.map{it.amount}.sum()
        val expenseAmount = totalAmount - budgetAmount

        balance.text = "$ %.2f".format(totalAmount)
        budget.text = "$ %.2f".format(budgetAmount)
        expense.text = "$ %.2f".format(expenseAmount)

        runBlocking {
            val preferences = DataStoreSingleton.getDataStore(applicationContext).data.first()
            val color = preferences[COLOR_KEY] ?: "red"

            val colorResId = when (color) {
                "red" -> R.color.red
                "green" -> R.color.green
                "blue" -> R.color.blue
                "yellow" -> R.color.yellow
                "purple" -> R.color.purple
                else -> R.color.red
            }

            balance.setTextColor(ContextCompat.getColor(this@MainActivity, colorResId))
        }
    }

    private fun undoDelete(){
        GlobalScope.launch {
            db.transactionDao().insertAll(deletedTransaction)

            transactions = oldTransactions

            runOnUiThread {
                transactionAdapter.setData(transactions)
                updateDashboard()
            }
        }
    }

    private fun showSnackbar(){
        val view = findViewById<View>(R.id.coordinator)
        val snackbar = Snackbar.make(view, "Transaction deleted!",Snackbar.LENGTH_LONG)
        snackbar.setAction("Undo"){
            undoDelete()
        }
            .setActionTextColor(ContextCompat.getColor(this, R.color.red))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    private fun deleteTransaction(transaction: Transaction){
        deletedTransaction = transaction
        oldTransactions = transactions

        GlobalScope.launch {
            db.transactionDao().delete(transaction)

            transactions = transactions.filter { it.id != transaction.id }
            runOnUiThread {
                updateDashboard()
                transactionAdapter.setData(transactions)
                showSnackbar()
            }
        }
    }

    private fun fetchTransactionsByPeriod(position: Int) {
        val calendar = Calendar.getInstance()
        when (position) {
            0 -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            1 -> calendar.add(Calendar.MONTH, -1)
            2 -> calendar.add(Calendar.MONTH, -2)
            3 -> calendar.timeInMillis = 0
        }
        val startDate = calendar.timeInMillis

        GlobalScope.launch {
            transactions = db.transactionDao().getTransactionsFromDate(startDate)
            runOnUiThread {
                updateDashboard()
                transactionAdapter.setData(transactions)
            }
        }
    }
}