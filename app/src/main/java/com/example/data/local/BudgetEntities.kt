package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BudgetItem
import com.example.data.model.CardPayment
import com.example.data.model.CreditCard
import com.example.data.model.EkasaAllocationStatus
import com.example.data.model.EkasaItemCodec
import com.example.data.model.SavedEkasaReceipt
import com.example.data.model.Transaction

@Entity(tableName = "budget_items")
data class BudgetItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val category: String,
    val amount: Double,
    val frequency: String,
    val startDate: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = BudgetItem(
        id = id,
        name = name,
        type = type,
        category = category,
        amount = amount,
        frequency = frequency,
        startDate = startDate
    )

    companion object {
        fun fromDomain(item: BudgetItem) = BudgetItemEntity(
            id = item.id,
            name = item.name,
            type = item.type,
            category = item.category,
            amount = item.amount,
            frequency = item.frequency,
            startDate = item.startDate
        )
    }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val category: String,
    val amount: Double,
    val date: String,
    val cardId: String = "",
    val ekasaReceiptId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Transaction(
        id = id,
        name = name,
        type = type,
        category = category,
        amount = amount,
        date = date,
        cardId = cardId,
        ekasaReceiptId = ekasaReceiptId
    )

    companion object {
        fun fromDomain(tx: Transaction) = TransactionEntity(
            id = tx.id,
            name = tx.name,
            type = tx.type,
            category = tx.category,
            amount = tx.amount,
            date = tx.date,
            cardId = tx.cardId,
            ekasaReceiptId = tx.ekasaReceiptId
        )
    }
}

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val limit: Double,
    val statementDay: Int,
    val dueDay: Int,
    val initialDebt: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = CreditCard(
        id = id,
        name = name,
        limit = limit,
        statementDay = statementDay,
        dueDay = dueDay,
        initialDebt = initialDebt
    )

    companion object {
        fun fromDomain(card: CreditCard) = CreditCardEntity(
            id = card.id,
            name = card.name,
            limit = card.limit,
            statementDay = card.statementDay,
            dueDay = card.dueDay,
            initialDebt = card.initialDebt
        )
    }
}

@Entity(tableName = "card_payments")
data class CardPaymentEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val date: String,
    val amount: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = CardPayment(
        id = id,
        cardId = cardId,
        date = date,
        amount = amount
    )

    companion object {
        fun fromDomain(payment: CardPayment) = CardPaymentEntity(
            id = payment.id,
            cardId = payment.cardId,
            date = payment.date,
            amount = payment.amount
        )
    }
}

@Entity(tableName = "app_config")
data class ConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "ekasa_receipts")
data class EkasaReceiptEntity(
    @PrimaryKey val receiptId: String,
    val organizationName: String,
    val issueDate: String,
    val totalPrice: Double,
    val itemsJson: String,
    val allocationStatus: String,
    val lastFetchedAt: Long,
    val updatedAt: Long
) {
    fun toDomain() = SavedEkasaReceipt(
        receiptId = receiptId,
        organizationName = organizationName,
        issueDate = issueDate,
        totalPrice = totalPrice,
        items = EkasaItemCodec.decodeItems(itemsJson),
        allocationStatus = EkasaAllocationStatus.fromString(allocationStatus),
        lastFetchedAt = lastFetchedAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(receipt: SavedEkasaReceipt) = EkasaReceiptEntity(
            receiptId = receipt.receiptId,
            organizationName = receipt.organizationName,
            issueDate = receipt.issueDate,
            totalPrice = receipt.totalPrice,
            itemsJson = EkasaItemCodec.encodeItems(receipt.items),
            allocationStatus = receipt.allocationStatus.name,
            lastFetchedAt = receipt.lastFetchedAt,
            updatedAt = receipt.updatedAt
        )
    }
}
