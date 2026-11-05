package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class EkasaReceiptData(
    val organizationName: String,
    val totalPrice: Double,
    val formattedDate: String, // "YYYY-MM-DD"
    val receiptId: String
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
     * Calls https://ekasa.financnasprava.sk/mdu/api/v1/opd/receipt/find
     * with payload {"receiptId": "..."} and parses receipt data.
     */
    suspend fun fetchReceipt(receiptId: String): Result<EkasaReceiptData> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("receiptId", receiptId)
            }.toString()

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

                val root = JSONObject(responseBody)
                val receiptObj = root.optJSONObject("receipt")
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("V odpovedi sa nenachádza platný doklad.")
                    )

                val organizationObj = receiptObj.optJSONObject("organization")
                val storeName = organizationObj?.optString("name")?.trim() ?: "Nákup"
                val totalPrice = receiptObj.optDouble("totalPrice", 0.0)
                val issueDateRaw = receiptObj.optString("issueDate", "")
                val formattedDate = normalizeDate(issueDateRaw)

                Result.success(
                    EkasaReceiptData(
                        organizationName = storeName,
                        totalPrice = totalPrice,
                        formattedDate = formattedDate,
                        receiptId = receiptId
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
