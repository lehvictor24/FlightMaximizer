package com.flightmaximizer.data.remote.kiwi

import com.flightmaximizer.data.local.datastore.AppPreferences
import com.flightmaximizer.data.remote.DataSourceException
import com.flightmaximizer.data.remote.FlightDataSource
import com.flightmaximizer.data.remote.model.FlightOffer
import com.flightmaximizer.data.remote.model.FlightSearchParams
import com.flightmaximizer.data.remote.util.DelayStrategy
import com.flightmaximizer.data.remote.util.UserAgentRotator
import com.flightmaximizer.domain.model.TripType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KiwiDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val prefs: AppPreferences,
    private val userAgentRotator: UserAgentRotator,
    private val delayStrategy: DelayStrategy
) : FlightDataSource {

    override val sourceName = "Kiwi.com"

    override suspend fun searchFlights(params: FlightSearchParams): List<FlightOffer> =
        withContext(Dispatchers.IO) {
            val apiKey = prefs.kiwiApiKey.first()
            if (apiKey.isBlank()) {
                Timber.w("Kiwi API key not set — skipping")
                return@withContext emptyList()
            }

            delayStrategy.preRequestDelay()

            val flightType = when (params.tripType) {
                TripType.ONE_WAY -> "oneway"
                TripType.ROUND_TRIP -> "round"
                TripType.MULTI_CITY -> "oneway"
            }

            // Kiwi expects dd/MM/yyyy format
            val dateFrom = formatKiwiDate(params.departDate.toString())
            val dateTo = dateFrom
            val returnDateStr = params.returnDate?.let { formatKiwiDate(it.toString()) }

            val url = "https://tequila.kiwi.com/v2/search" +
                "?fly_from=${params.origin}" +
                "&fly_to=${params.destination}" +
                "&date_from=$dateFrom" +
                "&date_to=$dateTo" +
                (if (returnDateStr != null) "&return_from=$returnDateStr&return_to=$returnDateStr" else "") +
                "&flight_type=$flightType" +
                "&curr=${params.currency}" +
                "&limit=5"

            val request = Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Accept", "application/json")
                .header("User-Agent", userAgentRotator.next())
                .build()

            try {
                val response = okHttpClient.newCall(request).execute()
                if (response.code == 429) throw DataSourceException.RateLimited()
                if (!response.isSuccessful) {
                    Timber.w("HTTP ${response.code} from $sourceName")
                    return@withContext emptyList()
                }

                val body = response.body?.string() ?: return@withContext emptyList()
                parseKiwiResponse(body, params)
            } catch (e: DataSourceException) {
                throw e
            } catch (e: IOException) {
                throw DataSourceException.NetworkError(e)
            } catch (e: Exception) {
                Timber.w(e, "Kiwi parse error")
                emptyList()
            }
        }

    private fun parseKiwiResponse(body: String, params: FlightSearchParams): List<FlightOffer> {
        val json = JSONObject(body)
        val data = json.optJSONArray("data") ?: return emptyList()
        val offers = mutableListOf<FlightOffer>()
        for (i in 0 until data.length()) {
            try {
                val item = data.getJSONObject(i)
                val price = (item.optDouble("price", 0.0) * 100).toInt()
                if (price <= 0) continue

                val departEpoch = item.optLong("dTimeUTC", 0L)
                val arriveEpoch = item.optLong("aTimeUTC", 0L)
                val depart = Instant.ofEpochSecond(departEpoch).atZone(ZoneOffset.UTC)
                val arrive = Instant.ofEpochSecond(arriveEpoch).atZone(ZoneOffset.UTC)
                val airline = item.optJSONArray("airlines")?.optString(0)
                val routeArray = item.optJSONArray("route")
                val stops = if (routeArray != null) (routeArray.length() - 1).coerceAtLeast(0) else 0
                val link = item.optString("deep_link").takeIf { it.isNotBlank() }

                offers.add(
                    FlightOffer(
                        origin = params.origin,
                        destination = params.destination,
                        priceCents = price,
                        currency = params.currency,
                        airline = airline,
                        flightNumber = null,
                        departDateTime = depart,
                        arriveDateTime = arrive,
                        returnDateTime = null,
                        durationMinutes = if (arriveEpoch > departEpoch) ((arriveEpoch - departEpoch) / 60).toInt() else null,
                        stops = stops,
                        bookingUrl = link,
                        tripType = params.tripType,
                        source = "kiwi"
                    )
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse Kiwi item $i")
            }
        }
        return offers
    }

    /**
     * Convert ISO date string (yyyy-MM-dd) to Kiwi's expected dd/MM/yyyy format.
     */
    private fun formatKiwiDate(isoDate: String): String {
        val parts = isoDate.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else isoDate
    }

    /**
     * Configure the API key directly (legacy compatibility — DataStore is preferred).
     */
    fun configure(key: String) {
        // no-op: key is read live from DataStore on every call
    }
}
