package gg.flyte.christmas.donate

import com.google.gson.JsonParser
import gg.flyte.twilight.scheduler.repeat
import gg.flyte.twilight.time.TimeUnit
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI

class RefreshToken(private val clientId: String, private val clientSecret: String) {

    companion object {
        lateinit var accessToken: String
    }

    private val url = URI("https://v5api.tiltify.com/oauth/token").toURL()
    private val requestBody = "client_id=$clientId" +
            "&client_secret=$clientSecret" +
            "&grant_type=client_credentials" +
            "&scope=public"

    /**
     * Requests a new access token every 7000 seconds.
     */
    init {
        repeat(7000, TimeUnit.SECONDS, true) { requestAccessToken() }
    }

    fun requestAccessToken(): Boolean {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.doOutput = true

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(requestBody)
            writer.flush()
        }

        return if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val responseJson = JsonParser.parseReader(InputStreamReader(connection.inputStream)).asJsonObject
            accessToken = responseJson.get("access_token").asString
            true
        } else {
            throw IllegalStateException("Failed to get access token: ${connection.responseCode}")
            false
        }
    }
}