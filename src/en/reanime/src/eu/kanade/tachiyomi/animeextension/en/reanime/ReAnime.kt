package eu.kanade.tachiyomi.animeextension.en.reanime

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.Exception

class ReAnime : ParsedAnimeHttpSource() {

    override val name = "ReAnime"
    override val baseUrl = "https://reanime.to"
    override val lang = "en"
    override val supportsLatest = true

    // ============================== POPULAR ANIME ==============================
    override fun popularAnimeRequest(page: Int): Request {
        return GET("$baseUrl/home", headers)
    }

    override fun popularAnimeSelector(): String = ".anime-card, .trending-item, div[data-slug]"

    override fun popularAnimeFromElement(element: Element): SAnime {
        return SAnime.create().apply {
            title = element.select("h3, .title, .anime-title").text().ifEmpty {
                element.attr("title")
            }
            val slug = element.attr("data-slug").ifEmpty {
                element.select("a").attr("href").removePrefix("/anime/").removePrefix("/")
            }
            setUrlWithoutDomain("/anime/$slug")
            thumbnail_url = element.select("img").attr("abs:src").ifEmpty {
                element.select("img").attr("abs:data-src")
            }
        }
    }

    override fun popularAnimeNextPageSelector(): String? = null

    // ============================== LATEST UPDATES ==============================
    override fun latestUpdatesRequest(page: Int): Request = popularAnimeRequest(page)
    override fun latestUpdatesSelector(): String = popularAnimeSelector()
    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)
    override fun latestUpdatesNextPageSelector(): String? = null

    // ============================== SEARCH ANIME ==============================
    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET("$baseUrl/search?q=$query", headers)
    }

    override fun searchAnimeSelector(): String = popularAnimeSelector()
    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)
    override fun searchAnimeNextPageSelector(): String? = null

    // ============================== ANIME DETAILS ==============================
    override fun animeDetailsParse(document: Document): SAnime {
        return SAnime.create().apply {
            title = document.select("h1.anime-title, .details-heading h1").text()
            genre = document.select(".genres a, .genre-item").joinToString(", ") { it.text() }
            description = document.select(".description, .synopsis").text()
            status = parseStatus(document.select(".status").text())
            thumbnail_url = document.select(".poster img, .anime-cover img").attr("abs:src")
        }
    }

    private fun parseStatus(status: String): Int {
        return when {
            status.contains("Currently Airing", ignoreCase = true) -> SAnime.ONGOING
            status.contains("Finished", ignoreCase = true) -> SAnime.COMPLETED
            else -> SAnime.UNKNOWN
        }
    }

    // ============================== EPISODES ==============================
    override fun episodeListSelector(): String = ".episode-item, .episodes-list a"

    override fun episodeFromElement(element: Element): SEpisode {
        return SEpisode.create().apply {
            val epNumText = element.select(".ep-num, .episode-number").text()
            name = element.text()
            setUrlWithoutDomain(element.attr("href"))
            episode_number = epNumText.replace("[^0-9.]".toRegex(), "").toFloatOrNull() ?: 1f
        }
    }

    // ============================== VIDEO EXTRACTOR ==============================
    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val videoList = mutableListOf<Video>()

        val iframeUrl = document.select("iframe#player, iframe.embed-player").attr("abs:src")

        if (iframeUrl.isNotEmpty()) {
            videoList.add(Video(iframeUrl, "ReAnime Server", iframeUrl))
        }

        return videoList
    }

    override fun videoFromElement(element: Element): Video = throw Exception("Not used")
    override fun videoUrlParse(document: Document): String = throw Exception("Not used")
    override fun videoListSelector(): String = throw Exception("Not used")
}
