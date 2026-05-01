package com.flightmaximizer.data.remote.google

import com.flightmaximizer.data.remote.DataSourceException
import com.flightmaximizer.data.remote.FlightDataSource
import com.flightmaximizer.data.remote.model.FlightOffer
import com.flightmaximizer.data.remote.model.FlightSearchParams
import com.flightmaximizer.data.remote.util.DelayStrategy
import com.flightmaximizer.data.remote.util.UserAgentRotator
import com.flightmaximizer.domain.model.TripType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleFlightsDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val urlBuilder: GoogleFlightsUrlBuilder,
    private val parser: GoogleFlightsParser,
    private val userAgentRotator: UserAgentRotator,
    private val delayStrategy: DelayStrategy
) : FlightDataSource {

    override val sourceName = "Google Flights"

    override suspend fun searchFlights(params: FlightSearchParams): List<FlightOffer> =
        withContext(Dispatchers.IO) {
            val url = urlBuilder.build(params)
            Timber.d("Scraping $sourceName: $url")

            delayStrategy.preRequestDelay()

            val html = fetch(url)
            val offers = parser.parse(html, params)
            Timber.d("$sourceName: ${offers.size} offers for ${params.origin}→${params.destination}")
            offers
        }

    /**
     * Fetch the Google Flights price calendar for a given month.
     * Used for FLEXIBLE and WHENEVER date modes to get cheapest fare per day.
     */
    suspend fun fetchCalendar(
        origin: String,
        dest: String,
        year: Int,
        month: Int,
        minNights: Int? = null,
        maxNights: Int? = null,
        currency: String = "USD"
    ): List<Pair<LocalDate, Int>> = withContext(Dispatchers.IO) {
        val url = urlBuilder.buildCalendarView(origin, dest, year, month, minNights, maxNights, currency)
        Timber.d("Fetching calendar: $url")
        val html = fetch(url)
        parser.parseCalendar(html, origin, dest)
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgentRotator.next())
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", userAgentRotator.nextAcceptLanguage())
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Cache-Control", "no-cache")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Connection", "keep-alive")
            .build()

        val response = try {
            okHttpClient.newCall(request).execute()
        } catch (e: IOException) {
            throw DataSourceException.NetworkError(e)
        }

        if (response.code == 429) {
            response.close()
            throw DataSourceException.RateLimited()
        }

        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            Timber.w("HTTP $code from $sourceName")
            throw DataSourceException.NetworkError(IOException("HTTP $code"))
        }

        return response.body?.string()
            ?: throw DataSourceException.ParseError("Empty response body")
    }
}
