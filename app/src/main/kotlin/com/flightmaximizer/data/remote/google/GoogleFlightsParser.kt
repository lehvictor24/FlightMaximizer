package com.flightmaximizer.data.remote.google

import com.flightmaximizer.data.remote.model.FlightOffer
import com.flightmaximizer.data.remote.model.FlightSearchParams
import org.jsoup.Jsoup
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleFlightsParser @Inject constructor(
    private val urlBuilder: GoogleFlightsUrlBuilder
) {

    companion object {
        private const val SOURCE = "google_flights"
        // Regex to find the AF_initDataCallback data island containing flight prices
        private val DATA_ISLAND_REGEX = Regex(
            """AF_initDataCallback\(\s*\{[^}]*"ds:1".*?data:([\s\S]*?),\s*sideChannel""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        // Regex to extract prices from aria-label attributes (fallback)
        private val PRICE_REGEX = Regex("""\$(\d[\d,]*)""")
        // Regex for calendar grid: date → price pairs
        private val CALENDAR_PRICE_REGEX = Regex("""(\d{4}-\d{2}-\d{2}).*?\$(\d[\d,]*)""")
    }

    fun parse(html: String, params: FlightSearchParams): List<FlightOffer> {
        return try {
            tryParseJsonDataIsland(html, params).ifEmpty {
                tryParseAriaLabels(html, params)
            }
        } catch (e: Exception) {
            Timber.w(e, "Parser failed for ${params.origin}→${params.destination}")
            emptyList()
        }
    }

    fun parseCalendar(html: String, origin: String, destination: String): List<Pair<LocalDate, Int>> {
        return tryParseCalendarGrid(html, origin, destination)
    }

    private fun tryParseJsonDataIsland(html: String, params: FlightSearchParams): List<FlightOffer> {
        return try {
            val doc = Jsoup.parse(html)
            val scripts = doc.select("script")
            for (script in scripts) {
                val text = script.data()
                if (text.contains("AF_initDataCallback") && text.contains("price")) {
                    val priceRegex = Regex("""\$(\d+(?:,\d+)?)""")
                    val prices = priceRegex.findAll(text).mapNotNull {
                        it.groupValues[1].replace(",", "").toIntOrNull()
                    }.filter { it in 50..50000 }.distinct().take(10).toList()

                    // Also try numeric values in the JSON blob
                    val jsonPrices = if (prices.isEmpty()) {
                        val pricePattern = Regex(""""(\d{2,6})"""")
                        pricePattern.findAll(text).mapNotNull { it.groupValues[1].toIntOrNull() }
                            .filter { it in 50..50000 }.distinct().take(10).toList()
                    } else prices

                    if (jsonPrices.isNotEmpty()) {
                        val airlinePattern = Regex(""""(American|Delta|United|Southwest|JetBlue|Alaska|Spirit|Frontier|British Airways|Air France|Lufthansa|Emirates|Qatar|Singapore)[^"]*"""")
                        val airline = airlinePattern.find(text)?.groupValues?.get(1)
                        return jsonPrices.mapIndexed { idx, price ->
                            buildOffer(params, price * 100, idx, airline, if (idx == 0) 0 else 1)
                        }
                    }
                }
            }
            emptyList()
        } catch (e: Exception) {
            Timber.w(e, "Data island parse failed")
            emptyList()
        }
    }

    private fun tryParseAriaLabels(html: String, params: FlightSearchParams): List<FlightOffer> {
        return try {
            val doc = Jsoup.parse(html)
            val offers = mutableListOf<FlightOffer>()

            doc.select("[aria-label]").forEach { el ->
                val label = el.attr("aria-label")
                val match = PRICE_REGEX.find(label)
                if (match != null) {
                    val price = match.groupValues[1].replace(",", "").toIntOrNull() ?: return@forEach
                    if (price in 50..50000) {
                        val airline = extractAirline(label)
                        val stops = extractStops(label)
                        offers.add(buildOffer(params, price * 100, offers.size, airline, stops))
                    }
                }
            }
            offers.take(10)
        } catch (e: Exception) {
            Timber.w(e, "Aria-label parse failed")
            emptyList()
        }
    }

    private fun tryParseCalendarGrid(
        html: String,
        origin: String,
        destination: String
    ): List<Pair<LocalDate, Int>> {
        return try {
            val doc = Jsoup.parse(html)
            // Calendar cells have aria-label like "Saturday, June 15, $342"
            doc.select("[aria-label]")
                .filter { it.attr("aria-label").matches(Regex(".*\\$\\d+.*")) }
                .mapNotNull { el ->
                    val label = el.attr("aria-label")
                    val priceMatch = PRICE_REGEX.find(label) ?: return@mapNotNull null
                    val price = priceMatch.groupValues[1].replace(",", "").toIntOrNull()
                        ?: return@mapNotNull null

                    val dateAttr = el.attr("data-date").ifEmpty { null }
                        ?: el.attr("data-iso").ifEmpty { null }

                    val date = if (dateAttr != null) {
                        runCatching { LocalDate.parse(dateAttr) }.getOrNull()
                    } else null

                    date?.let { it to price * 100 }
                }
        } catch (e: Exception) {
            Timber.w(e, "Calendar grid parse failed")
            emptyList()
        }
    }

    private fun buildOffer(
        params: FlightSearchParams,
        priceCents: Int,
        index: Int,
        airline: String? = null,
        stops: Int = 0
    ): FlightOffer {
        val depart: ZonedDateTime = params.departDate.atStartOfDay(ZoneOffset.UTC).plusHours(8 + index.toLong())
        val arrive: ZonedDateTime = depart.plusHours(8)
        val returnDt: ZonedDateTime? = params.returnDate?.atStartOfDay(ZoneOffset.UTC)?.plusHours(10)
        return FlightOffer(
            origin = params.origin,
            destination = params.destination,
            priceCents = priceCents,
            currency = params.currency,
            airline = airline,
            flightNumber = null,
            departDateTime = depart,
            arriveDateTime = arrive,
            returnDateTime = returnDt,
            durationMinutes = 480,
            stops = stops,
            bookingUrl = buildBookingUrl(params),
            tripType = params.tripType,
            source = SOURCE
        )
    }

    private fun buildBookingUrl(params: FlightSearchParams): String =
        urlBuilder.buildBookingUrl(params)

    private fun extractAirline(label: String): String? {
        val airlines = listOf(
            "Delta", "American", "United", "Southwest", "JetBlue", "Alaska",
            "Spirit", "Frontier", "British Airways", "Lufthansa", "Air France", "Emirates"
        )
        return airlines.firstOrNull { label.contains(it, ignoreCase = true) }
    }

    private fun extractStops(label: String): Int {
        return when {
            label.contains("nonstop", ignoreCase = true) -> 0
            label.contains("1 stop", ignoreCase = true) -> 1
            label.contains("2 stop", ignoreCase = true) -> 2
            else -> 0
        }
    }
}
