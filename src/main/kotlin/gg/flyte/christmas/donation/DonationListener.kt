package gg.flyte.christmas.donation

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.flyte.christmas.ChristmasEventPlugin
import gg.flyte.twilight.scheduler.sync
import kotlinx.coroutines.*
import kotlinx.io.IOException
import org.bukkit.Bukkit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.Throws
import kotlin.math.roundToInt

/**
 * A listener that continuously fetches donation data from the API and fires a [DonateEvent] for each unique donation.
 */
class DonationListener {
    private val url: URL = URI.create("https://blue-sky-057da630f.5.azurestaticapps.net/service/all").toURL()
    private val processedDonations = mutableSetOf<String>()

    init {
        fetchDonations()
    }

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
                    val data = requestDonationDataAsJson()
                    ChristmasEventPlugin.instance.eventController.apply {
                        donationGoal = data.get("goal").asDouble.roundToInt()
                        totalDonations = data.get("amount").asDouble.roundToInt()
                        updateDonationBar()
                    }
                    submitDataToEventFactory(data.getAsJsonArray("donations"))
                } catch (e: Exception) {
                    ChristmasEventPlugin.instance.logger.severe("Failed to fetch donations: ${e.message}")
                }
                delay(5_000) // 5 secs
            }
        }
    }

    /**
     * Makes a GET request to fetch the donation data from the API.
     *
     * @return The JSON response as a JsonObject.
     * @throws IOException If an input or output exception occurred while reading from the connection stream.
     */
    @Throws(IOException::class)
    private fun requestDonationDataAsJson(): JsonObject {
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Content-Type", "application/json")

        val data: JsonElement
        BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
            data = JsonParser.parseReader(reader)
        }

        if (data.isJsonObject) return data.asJsonObject
        else throw RuntimeException("Invalid response!")
    }

    /**
     * Handles the donation data received by processing each donation and fires a [DonateEvent] for each new donation.
     * @param donationsData A JsonObject containing an array of donation data.
     */
    private fun submitDataToEventFactory(donations: JsonArray) {
        donations.forEach { donationElement ->
            val donation = donationElement.asJsonObject
            val donationId = donation.get("id").asString
            if (processedDonations.add(donationId)) {
                val donorName = donation.get("name")?.asString
                val comment = donation.get("comment")?.asString
                val amount = donation.get("amount").asString
                val timestamp = donation.get("timestamp").asLong

                sync {
                    Bukkit.getPluginManager().callEvent(
                        DonateEvent(
                            donationId,
                            donorName,
                            comment,
                            amount,
                            timestamp
                        )
                    )
                }
            }
        }
    }
}