package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // Config
    @Query("SELECT * FROM app_config WHERE `key` = :key LIMIT 1")
    fun getConfigFlow(key: String): Flow<ConfigEntity?>

    @Query("SELECT * FROM app_config WHERE `key` = :key LIMIT 1")
    suspend fun getConfig(key: String): ConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: ConfigEntity)

    // Planned Budget Items
    @Query("SELECT * FROM budget_items ORDER BY type ASC, name ASC")
    fun getAllBudgetItems(): Flow<List<BudgetItemEntity>>

    @Query("SELECT * FROM budget_items")
    suspend fun getAllBudgetItemsDirect(): List<BudgetItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetItem(item: BudgetItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetItems(items: List<BudgetItemEntity>)

    @Query("DELETE FROM budget_items WHERE id = :id")
    suspend fun deleteBudgetItem(id: String)

    @Query("DELETE FROM budget_items")
    suspend fun clearBudgetItems()

    // Actual Transactions
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsDirect(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    // Credit Cards
    @Query("SELECT * FROM credit_cards ORDER BY name ASC")
    fun getAllCreditCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards")
    suspend fun getAllCreditCardsDirect(): List<CreditCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(card: CreditCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCards(cards: List<CreditCardEntity>)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteCreditCard(id: String)

    @Query("DELETE FROM credit_cards")
    suspend fun clearCreditCards()

    // Card Payments
    @Query("SELECT * FROM card_payments ORDER BY date DESC, createdAt DESC")
    fun getAllCardPayments(): Flow<List<CardPaymentEntity>>

    @Query("SELECT * FROM card_payments")
    suspend fun getAllCardPaymentsDirect(): List<CardPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardPayment(payment: CardPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardPayments(payments: List<CardPaymentEntity>)

    @Query("DELETE FROM card_payments WHERE id = :id")
    suspend fun deleteCardPayment(id: String)

    @Query("DELETE FROM card_payments WHERE cardId = :cardId")
    suspend fun deleteCardPaymentsByCard(cardId: String)

    @Query("UPDATE transactions SET cardId = '' WHERE cardId = :cardId")
    suspend fun unassignCardFromTransactions(cardId: String)

    @Query("DELETE FROM card_payments")
    suspend fun clearCardPayments()

    // Cascade delete card
    @Transaction
    suspend fun deleteCardAndRelated(cardId: String) {
        deleteCreditCard(cardId)
        deleteCardPaymentsByCard(cardId)
        unassignCardFromTransactions(cardId)
    }

    // Reset entire dataset
    @Transaction
    suspend fun clearAllData() {
        clearBudgetItems()
        clearTransactions()
        clearCreditCards()
        clearCardPayments()
        setConfig(ConfigEntity("initial_balance", "0.0"))
    }

    // Import replace dataset
    @Transaction
    suspend fun replaceFullData(
        initialBalance: Double,
        items: List<BudgetItemEntity>,
        transactions: List<TransactionEntity>,
        cards: List<CreditCardEntity>,
        payments: List<CardPaymentEntity>
    ) {
        clearAllData()
        setConfig(ConfigEntity("initial_balance", initialBalance.toString()))
        insertBudgetItems(items)
        insertTransactions(transactions)
        insertCreditCards(cards)
        insertCardPayments(payments)
    }
}
