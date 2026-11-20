package com.musicforme.app.data.remote

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.musicforme.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Native on-device YouTube Music InnerTube Extractor.
 * Searches full songs via YouTube Music (WEB_REMIX) and extracts direct high-bitrate
 * M4A/AAC audio streams via the ANDROID_VR client context without needing external proxy servers.
 */
object YouTubeMusicExtractor {

    private const val TAG = "YouTubeMusicExtractor"
    const val YT_SCHEME = "yt://"
    const val VR_USER_AGENT =
        "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
    private const val WEB_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Volatile
    private var cachedVisitorData: String? = null

    private data class CachedStream(val url: String, val expiresAtMs: Long)
    private val streamCache = ConcurrentHashMap<String, CachedStream>()

    /**
     * Search full-length songs on YouTube Music.
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val searchTerm = query.trim().ifBlank { "nhạc trẻ hot nhất hiện nay" }

        val requestJson = JsonObject().apply {
            add("context", JsonObject().apply {
                add("client", JsonObject().apply {
                    addProperty("clientName", "WEB_REMIX")
                    addProperty("clientVersion", "1.20250310.01.00")
                    addProperty("hl", "vi")
                    addProperty("gl", "VN")
                })
            })
            addProperty("query", searchTerm)
            // Filter strictly for Songs on YouTube Music
            addProperty("params", "EgWKAQIIAWoMEAMQBBAJEAoQBRAV")
        }

        val request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/search")
            .header("User-Agent", WEB_USER_AGENT)
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("YouTube Music search failed: HTTP ${response.code}")
            }
            val bodyString = response.body?.string() ?: return@withContext emptyList()
            parseSearchResults(bodyString)
        }
    }

    private fun parseSearchResults(jsonString: String): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            // Cache visitorData if returned by search
            root.getAsJsonObject("responseContext")
                ?.get("visitorData")?.asString
                ?.takeIf { it.isNotBlank() }
                ?.let { cachedVisitorData = it }

            val tabs = root.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return songs

            val sectionContents = tabs.firstOrNull()?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return songs

            for (sectionElement in sectionContents) {
                val shelf = sectionElement.asJsonObject.getAsJsonObject("musicShelfRenderer") ?: continue
                val items = shelf.getAsJsonArray("contents") ?: continue

                for (itemElement in items) {
                    val renderer = itemElement.asJsonObject
                        .getAsJsonObject("musicResponsiveListItemRenderer") ?: continue

                    val videoId = renderer.getAsJsonObject("playlistItemData")
                        ?.get("videoId")?.asString
                        ?: extractVideoIdFromOverlay(renderer)
                        ?: continue

                    val flexColumns = renderer.getAsJsonArray("flexColumns") ?: continue
                    val title = extractRunsText(flexColumns, 0) ?: continue
                    val subtitleRuns = extractRunsList(flexColumns, 1)

                    // Parse subtitle parts separated by " • " (Artist • Album • Duration)
                    val parts = subtitleRuns
                        .filter { it.trim() != "•" && it.trim() != "Bài hát" && it.trim() != "Song" }
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    var durationMs = 0L
                    var artist = "YouTube Music"
                    var album = "YouTube Music"

                    if (parts.isNotEmpty()) {
                        val lastPart = parts.last()
                        val parsedDur = parseDurationTextToMs(lastPart)
                        val metadataParts = if (parsedDur > 0L) {
                            durationMs = parsedDur
                            parts.dropLast(1)
                        } else {
                            parts
                        }

                        if (metadataParts.isNotEmpty()) {
                            artist = metadataParts.first()
                        }
                        if (metadataParts.size >= 2) {
                            album = metadataParts.last()
                        }
                    }

                    val rawThumb = renderer.getAsJsonObject("thumbnail")
                        ?.getAsJsonObject("musicThumbnailRenderer")
                        ?.getAsJsonObject("thumbnail")
                        ?.getAsJsonArray("thumbnails")
                        ?.lastOrNull()?.asJsonObject
                        ?.get("url")?.asString

                    // Upgrade thumbnail to high-res 544x544 square artwork
                    val highResThumb = rawThumb?.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                    songs.add(
                        Song(
                            id = "yt_$videoId",
                            title = title,
                            artist = artist,
                            album = album,
                            duration = durationMs,
                            uriString = "$YT_SCHEME$videoId",
                            albumArtUri = highResThumb,
                            isOnline = true,
                            downloadUrl = "$YT_SCHEME$videoId"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse YouTube Music search results", e)
        }
        return songs
    }

    private fun extractVideoIdFromOverlay(renderer: JsonObject): String? {
        return renderer.getAsJsonObject("overlay")
            ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
            ?.getAsJsonObject("content")
            ?.getAsJsonObject("musicPlayButtonRenderer")
            ?.getAsJsonObject("playNavigationEndpoint")
            ?.getAsJsonObject("watchEndpoint")
            ?.get("videoId")?.asString
    }

    private fun extractRunsText(flexColumns: JsonArray, index: Int): String? {
        if (index >= flexColumns.size()) return null
        val runs = flexColumns[index].asJsonObject
            .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            ?.getAsJsonObject("text")
            ?.getAsJsonArray("runs") ?: return null
        return runs.joinToString("") { it.asJsonObject.get("text")?.asString.orEmpty() }
            .takeIf { it.isNotBlank() }
    }

    private fun extractRunsList(flexColumns: JsonArray, index: Int): List<String> {
        if (index >= flexColumns.size()) return emptyList()
        val runs = flexColumns[index].asJsonObject
            .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            ?.getAsJsonObject("text")
            ?.getAsJsonArray("runs") ?: return emptyList()
        return runs.mapNotNull { it.asJsonObject.get("text")?.asString }
    }

    private fun parseDurationTextToMs(text: String): Long {
        val parts = text.split(":")
        if (parts.size !in 2..3) return 0L
        return try {
            if (parts.size == 2) {
                val mins = parts[0].toLong()
                val secs = parts[1].toLong()
                (mins * 60 + secs) * 1000L
            } else {
                val hours = parts[0].toLong()
                val mins = parts[1].toLong()
                val secs = parts[2].toLong()
                (hours * 3600 + mins * 60 + secs) * 1000L
            }
        } catch (e: NumberFormatException) {
            0L
        }
    }

    private fun ensureVisitorDataSync(): String {
        cachedVisitorData?.let { return it }

        val reqJson = JsonObject().apply {
            add("context", JsonObject().apply {
                add("client", JsonObject().apply {
                    addProperty("clientName", "WEB_REMIX")
                    addProperty("clientVersion", "1.20250310.01.00")
                })
            })
        }

        val request = Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/visitor_id")
            .header("User-Agent", WEB_USER_AGENT)
            .post(reqJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw IOException("Empty visitor_id response")
            val vd = JsonParser.parseString(body).asJsonObject
                .getAsJsonObject("responseContext")
                ?.get("visitorData")?.asString
                ?: throw IOException("Missing visitorData")
            cachedVisitorData = vd
            return vd
        }
    }

    /**
     * Synchronously resolves a YouTube videoId into a direct playable M4A/AAC audio stream URL.
     * Intended to be called on background IO threads (e.g. ExoPlayer's ResolvingDataSource).
     */
    @Throws(IOException::class)
    fun resolveAudioStreamUrlSync(videoId: String): String {
        val now = System.currentTimeMillis()
        streamCache[videoId]?.let { cached ->
            if (cached.expiresAtMs > now) {
                return cached.url
            }
        }

        val visitorData = ensureVisitorDataSync()

        val playerJson = JsonObject().apply {
            add("context", JsonObject().apply {
                add("client", JsonObject().apply {
                    addProperty("clientName", "ANDROID_VR")
                    addProperty("clientVersion", "1.65.10")
                    addProperty("deviceMake", "Oculus")
                    addProperty("deviceModel", "Quest 3")
                    addProperty("androidSdkVersion", 32)
                    addProperty("osName", "Android")
                    addProperty("osVersion", "12L")
                    addProperty("hl", "vi")
                    addProperty("gl", "VN")
                    addProperty("visitorData", visitorData)
                })
            })
            addProperty("videoId", videoId)
            addProperty("contentCheckOk", true)
            addProperty("racyCheckOk", true)
        }

        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player")
            .header("User-Agent", VR_USER_AGENT)
            .header("X-Goog-Visitor-Id", visitorData)
            .post(playerJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("YouTube player request failed: HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty player response")
            val root = JsonParser.parseString(body).asJsonObject

            val status = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            if (status != "OK") {
                val reason = root.getAsJsonObject("playabilityStatus")?.get("reason")?.asString
                throw IOException("Track not playable ($status): $reason")
            }

            val adaptiveFormats = root.getAsJsonObject("streamingData")
                ?.getAsJsonArray("adaptiveFormats")
                ?: throw IOException("No adaptiveFormats in player response")

            var bestMp4Url: String? = null
            var bestMp4Bitrate = -1
            var fallbackAudioUrl: String? = null
            var fallbackBitrate = -1

            for (el in adaptiveFormats) {
                val fmt = el.asJsonObject
                val mime = fmt.get("mimeType")?.asString ?: continue
                val url = fmt.get("url")?.asString ?: continue
                val bitrate = fmt.get("bitrate")?.asInt ?: 0

                if (mime.startsWith("audio/mp4")) {
                    if (bitrate > bestMp4Bitrate) {
                        bestMp4Bitrate = bitrate
                        bestMp4Url = url
                    }
                } else if (mime.startsWith("audio/")) {
                    if (bitrate > fallbackBitrate) {
                        fallbackBitrate = bitrate
                        fallbackAudioUrl = url
                    }
                }
            }

            val finalUrl = bestMp4Url ?: fallbackAudioUrl
                ?: throw IOException("No direct audio stream URL found for $videoId")

            // Cache for 3 hours (YouTube stream URLs typically remain valid for ~6 hours)
            streamCache[videoId] = CachedStream(finalUrl, now + TimeUnit.HOURS.toMillis(3))
            return finalUrl
        }
    }

    suspend fun resolveAudioStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            resolveAudioStreamUrlSync(videoId)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving YouTube audio URL for $videoId", e)
            null
        }
    }

    fun extractVideoId(uriString: String): String? {
        return if (uriString.startsWith(YT_SCHEME)) {
            uriString.removePrefix(YT_SCHEME).trim().takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }
}
