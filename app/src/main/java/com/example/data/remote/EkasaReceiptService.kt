package com.example.data.remote

import com.example.data.model.EkasaLineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class EkasaReceiptData(
    val organizationName: String,
    val totalPrice: Double,
    val formattedDate: String, // "YYYY-MM-DD"
    val receiptId: String,
    val items: List<EkasaLineItem> = emptyList()
)

object EkasaReceiptService {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val moshi: Moshi by lazy { Moshi.Builder().build() }
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter by lazy { moshi.adapter<Map<String, Any?>>(mapType) }

    /**
     * Extracts the 34-character eKasa UID (starting with O- or P- followed by 32 hex chars)
     * from any text, URL or raw QR content.
     */
    fun extractReceiptId(qrContent: String): String? {
        val regex = Regex("([OP]-[0-9A-Fa-f]{32})", RegexOption.IGNORE_CASE)
        val match = regex.find(qrContent)
        return match?.value?.uppercase()
    }

    /**
     * Normalizes dates like "05.11.2026 14:30:00" or "2026-11-05T14:30:00" to "YYYY-MM-DD".
     */
    fun normalizeDate(rawDate: String): String {
        val trimmed = rawDate.trim()
        val ymdMatch = Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(trimmed)
        if (ymdMatch != null) {
            return ymdMatch.value
        }
        val dmyMatch = Regex("^(\\d{1,2})\\.\\s*(\\d{1,2})\\.\\s*(\\d{4})").find(trimmed)
        if (dmyMatch != null) {
            val (d, m, y) = dmyMatch.destructured
            return String.format(Locale.US, "%04d-%02d-%02d", y.toInt(), m.toInt(), d.toInt())
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    /**
     * Parses raw JSON response from Finančná správa eKasa API into EkasaReceiptData including items[].
     */
    fun parseReceiptJson(responseBody: String, fallbackReceiptId: String): Result<EkasaReceiptData> {
        return try {
            val root = mapAdapter.fromJson(responseBody)
                ?: return Result.failure(
                    IllegalArgumentException("V odpovedi sa nenachádza platný doklad.")
                )
            val receiptObj = root["receipt"] as? Map<*, *>
                ?: return Result.failure(
                    IllegalArgumentException("V odpovedi sa nenachádza platný doklad.")
                )

            val organizationObj = receiptObj["organization"] as? Map<*, *>
            val storeName = (organizationObj?.get("name") as? String)?.trim()?.ifEmpty { "Nákup" } ?: "Nákup"
            val totalPrice = (receiptObj["totalPrice"] as? Number)?.toDouble() ?: 0.0
            val issueDateRaw = (receiptObj["issueDate"] as? String) ?: ""
            val formattedDate = normalizeDate(issueDateRaw)
            val parsedId = (receiptObj["receiptId"] as? String)?.ifBlank { fallbackReceiptId } ?: fallbackReceiptId

            val itemsList = mutableListOf<EkasaLineItem>()
            val itemsArr = receiptObj["items"] as? List<*>
            if (itemsArr != null) {
                itemsArr.forEachIndexed { i, rawItem ->
                    val itemObj = rawItem as? Map<*, *> ?: return@forEachIndexed
                    val itemName = (itemObj["name"] as? String)?.trim()?.ifEmpty { "Položka ${i + 1}" }
                        ?: "Položka ${i + 1}"
                    val qty = (itemObj["quantity"] as? Number)?.toDouble() ?: 1.0
                    val price = (itemObj["price"] as? Number)?.toDouble() ?: 0.0
                    val itemType = (itemObj["itemType"] as? String)?.ifBlank { "K" } ?: "K"
                    itemsList.add(
                        EkasaLineItem(
                            index = i,
                            name = itemName,
                            quantity = qty,
                            price = price,
                            itemType = itemType
                        )
                    )
                }
            }

            Result.success(
                EkasaReceiptData(
                    organizationName = storeName,
                    totalPrice = totalPrice,
                    formattedDate = formattedDate,
                    receiptId = parsedId,
                    items = itemsList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Merges freshly fetched line items with previously stored items so any already-assigned
     * category allocations on a partially split receipt are preserved.
     */
    fun mergeFetchedItemsWithExisting(
        fetched: List<EkasaLineItem>,
        existing: List<EkasaLineItem>
    ): List<EkasaLineItem> {
        if (existing.isEmpty()) return fetched
        val existingByIndex = existing.associateBy { it.index }
        return fetched.map { fresh ->
            val old = existingByIndex[fresh.index]
            if (old != null && old.assignedCategory.isNotBlank() && old.name == fresh.name) {
                fresh.copy(assignedCategory = old.assignedCategory)
            } else {
                fresh
            }
        }
    }

    /**
     * Calls https://ekasa.financnasprava.sk/mdu/api/v1/opd/receipt/find
     * with payload {"receiptId": "..."} and parses receipt metadata and items.
     */
    suspend fun fetchReceipt(receiptId: String): Result<EkasaReceiptData> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = mapAdapter.toJson(mapOf("receiptId" to receiptId))

            val request = Request.Builder()
                .url("https://ekasa.financnasprava.sk/mdu/api/v1/opd/receipt/find")
                .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .header("User-Agent", "RodinnyRozpocet-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    return@withContext Result.failure(
                        IOException("Server Finančnej správy vrátil chybu ($code).")
                    )
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Server vrátil prázdnu odpoveď."))

                parseReceiptJson(responseBody, receiptId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
