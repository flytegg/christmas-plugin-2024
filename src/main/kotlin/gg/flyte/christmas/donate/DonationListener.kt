package gg.flyte.christmas.donate

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.christmas.donate.RefreshToken.Companion.accessToken
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import org.bukkit.Bukkit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class DonationListener(private val campaignId: String) {
    private val url: URL = URI.create("https://v5api.tiltify.com/api/public/campaigns/$campaignId/donations").toURL()
    private val processedDonations = mutableSetOf<String>()
    
    init { fetchDonations() }
    /**
     * Continuously fetches donation data for a specified campaign at a 10-second interval.
     *
     * @param campaignId The ID of the campaign for which donations are being fetched.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun fetchDonations() {
        GlobalScope.launch {
            while (isActive) {
                try {
                    val donationsData = requestJson()
                    donationHandler(donationsData)
                } catch (e: Exception) {
                    ChristmasEventPlugin.instance.logger.severe("Failed to fetch donations: ${e.message}")
                }
                delay(10_000) // 10 secs
            }
        }
    }

    /**
     * Makes a GET request to fetch the donation data from the Tiltify API.
     *
     * @return The JSON response as a JsonObject.
     * @throws IOException If an input or output exception occurred while reading from the connection stream.
     */
    @Throws(IOException::class)
    private fun requestJson(): JsonObject {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("Content-Type", "application/json")

        val data: JsonElement
        BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
            data = JsonParser.parseReader(reader)
        }

        if (data.isJsonObject) return data.asJsonObject
        else throw RuntimeException("Invalid response!")
    }

    /**
     * Handles the donation data received by processing each donation and fires a [DonateEvent] for each unique donation.
     * @param donationsData A JsonObject containing an array of donation data.
     */
    private fun donationHandler(donationsData: JsonObject) {
        val dataArray = donationsData.getAsJsonArray("data")
        dataArray.forEach { donationElement ->
            val donation = donationElement.asJsonObject
            val donationId = donation.get("id").asString
            if (processedDonations.add(donationId)) {
                val donorName = donation.get("donor_name")?.asString ?: "Anonymous"
                val comment = donation.get("donor_comment")?.asString ?: ""
                val amount = donation.getAsJsonObject("amount")
                val time = donation.get("completed_at")?.asString ?: ""
                val value = amount.get("value")?.asString ?: ""
                val currency = amount.get("currency")?.asString ?: "USD"
                Bukkit.getPluginManager().callEvent(DonateEvent(donorName, comment, time, value, currency, donationId))
            }
        }
    }
}