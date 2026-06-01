package com.profconq.app.youtube

data class TranscriptSegment(
    val text: String,
    val offsetSec: Float,
    val durationSec: Float,
)

data class SubtitleLine(
    val id: String,
    val text: String,
    val startSec: Float,
    val endSec: Float,
)

data class TranscriptResult(
    val lines: List<SubtitleLine>,
    val language: String,
)

data class YouTubeVideoResult(
    val videoId: String,
    val title: String,
    val channel: String,
    val duration: String?,
    val thumbnailUrl: String?,
    val isShort: Boolean = false,
)

enum class VideoSearchFilter {
    All,
    Full,
    Shorts,
}

enum class YouTubeInputMode {
    Search,
    Link,
}

data class YouTubeWatchHistoryItem(
    val videoId: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String?,
    val duration: String?,
    val isShort: Boolean,
    val lastWatchedAt: Long,
    val lastPositionSec: Float,
    val watchCount: Int,
) {
    fun toVideoResult(): YouTubeVideoResult = YouTubeVideoResult(
        videoId = videoId,
        title = title,
        channel = channel,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        isShort = isShort,
    )
}
