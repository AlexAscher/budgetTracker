package com.alterpat.budgettracker

import androidx.room.*

@Dao
interface TransactionDao {
    @Query("SELECT * from transactions")
    fun getAll(): List<Transaction>

    @Query("SELECT * from transactions WHERE amount > 0")
    fun getPositiveTransactions(): List<Transaction>

    @Query("SELECT * from transactions WHERE amount < 0")
    fun getNegativeTransactions(): List<Transaction>

    @Query("SELECT * from transactions WHERE date >= :startDate")
    fun getTransactionsFromDate(startDate: Long): List<Transaction>

    @Insert
    fun insertAll(vararg transaction: Transaction)

    @Delete
    fun delete(transaction: Transaction)

    @Update
    fun update(vararg transaction: Transaction)
}