package com.wxy.playerlite.feature.player

import com.wxy.playerlite.network.core.JsonHttpClient
import com.wxy.playerlite.network.core.NetworkRequestException
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal interface LyricRepository {
    suspend fun readCachedLyrics(songId: String): ParsedLyrics?

    suspend fun fetchLyrics(songId: String): ParsedLyrics?
}

internal class DefaultLyricRepository(
    private val remoteDataSource: LyricRemoteDataSource,
    private val localStore: LyricLocalStore
) : LyricRepository {
    override suspend fun readCachedLyrics(songId: String): ParsedLyrics? {
        val rawLyric = localStore.read(songId) ?: return null
        return LyricParser.parse(songId = songId, rawLyric = rawLyric)
    }

    override suspend fun fetchLyrics(songId: String): ParsedLyrics? {
        val payload = fetchPayloadWithRetry(songId)
        val rawLyric = payload.objectValue("lrc").stringValue("lyric").orEmpty()
        val parsed = LyricParser.parse(songId = songId, rawLyric = rawLyric) ?: return null
        localStore.write(songId = songId, rawLyric = rawLyric)
        return parsed
    }

    private suspend fun fetchPayloadWithRetry(songId: String): JsonObject {
        var attemptIndex = 0
        while (true) {
            val payload = try {
                remoteDataSource.fetchLyrics(songId)
            } catch (error: NetworkRequestException) {
                val retryDelayMs = LYRIC_REQUEST_RETRY_DELAYS_MS.getOrNull(attemptIndex)
                if (retryDelayMs == null || !error.isRetryableLyricRequestFailure()) {
                    throw error
                }
                delay(retryDelayMs)
                attemptIndex += 1
                continue
            }
            val responseCode = payload.intValue("code")
            if (responseCode == 200) {
                return payload
            }
            val retryDelayMs = LYRIC_REQUEST_RETRY_DELAYS_MS.getOrNull(attemptIndex)
            if (retryDelayMs == null || !payload.isRetryableLyricResponse()) {
                throw IllegalStateException("Lyric request failed: code=$responseCode")
            }
            delay(retryDelayMs)
            attemptIndex += 1
        }
    }
}

internal interface LyricRemoteDataSource {
    suspend fun fetchLyrics(songId: String): JsonObject
}

internal class NeteaseLyricRemoteDataSource(
    private val httpClient: JsonHttpClient
) : LyricRemoteDataSource {
    override suspend fun fetchLyrics(songId: String): JsonObject {
        return httpClient.get(
            path = "/lyric",
            queryParams = mapOf("id" to songId),
            requiresAuth = false
        )
    }
}

internal class LyricLocalStore(
    private val directory: File,
    private val maxEntries: Int = 100
) {
    fun read(songId: String): String? {
        val file = resolveFile(songId)
        if (!file.exists()) {
            return null
        }
        file.setLastModified(System.currentTimeMillis())
        return file.readText()
    }

    fun write(songId: String, rawLyric: String) {
        directory.mkdirs()
        val file = resolveFile(songId)
        file.writeText(rawLyric)
        file.setLastModified(System.currentTimeMillis())
        pruneIfNeeded()
    }

    private fun pruneIfNeeded() {
        val files = directory.listFiles { candidate ->
            candidate.isFile && candidate.extension == FILE_EXTENSION
        }?.toList().orEmpty()
        if (files.size <= maxEntries) {
            return
        }
        files.sortedBy { it.lastModified() }
            .take(files.size - maxEntries)
            .forEach(File::delete)
    }

    private fun resolveFile(songId: String): File {
        return File(directory, "$songId.$FILE_EXTENSION")
    }

    private companion object {
        private const val FILE_EXTENSION = "lrc"
    }
}

internal object LyricParser {
    private val timestampPattern = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{1,3}))?]""")

    fun parse(songId: String, rawLyric: String): ParsedLyrics? {
        val normalized = rawLyric.trim()
        if (normalized.isBlank()) {
            return null
        }
        val lines = buildList {
            normalized.lineSequence().forEach { rawLine ->
                val matches = timestampPattern.findAll(rawLine).toList()
                if (matches.isEmpty()) {
                    return@forEach
                }
                val text = timestampPattern.replace(rawLine, "").trim()
                if (text.isBlank()) {
                    return@forEach
                }
                matches.forEach { match ->
                    add(
                        LyricLine(
                            timestampMs = parseTimestampMs(match),
                            text = text
                        )
                    )
                }
            }
        }.sortedBy { it.timestampMs }
        if (lines.isEmpty()) {
            return null
        }
        return ParsedLyrics(
            songId = songId,
            lines = lines,
            rawText = normalized
        )
    }

    private fun parseTimestampMs(match: MatchResult): Long {
        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
        val fraction = match.groupValues.getOrElse(3) { "" }
        val fractionMs = when (fraction.length) {
            1 -> fraction.toLongOrNull()?.times(100L) ?: 0L
            2 -> fraction.toLongOrNull()?.times(10L) ?: 0L
            3 -> fraction.toLongOrNull() ?: 0L
            else -> 0L
        }
        return minutes * 60_000L + seconds * 1_000L + fractionMs
    }
}

private fun JsonObject.objectValue(key: String): JsonObject {
    return this[key] as? JsonObject ?: JsonObject(emptyMap())
}

private fun JsonObject.stringValue(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}

private fun JsonObject.intValue(key: String): Int {
    return stringValue(key)?.toIntOrNull() ?: 0
}

private fun JsonObject.isRetryableLyricResponse(): Boolean {
    return intValue("code").isRetryableLyricStatusCode() ||
        intValue(JsonHttpClient.KEY_HTTP_STATUS).isRetryableLyricStatusCode()
}

private fun NetworkRequestException.isRetryableLyricRequestFailure(): Boolean {
    val status = statusCode ?: return true
    return status in 200..299 || status.isRetryableLyricStatusCode()
}

private fun Int.isRetryableLyricStatusCode(): Boolean {
    return this == 408 || this == 425 || this == 429 || this in 500..599
}

private val LYRIC_REQUEST_RETRY_DELAYS_MS = longArrayOf(250L, 750L)
