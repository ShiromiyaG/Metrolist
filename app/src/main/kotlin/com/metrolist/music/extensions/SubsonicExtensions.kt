package com.metrolist.music.extensions

import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.subsonic.models.Album
import com.metrolist.subsonic.models.AlbumWithSongs
import com.metrolist.subsonic.models.Artist
import com.metrolist.subsonic.models.Song
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Extension functions to convert Subsonic models to Room entities
 */

/**
 * Convert Subsonic Song to SongEntity
 */
fun Song.toSongEntity(): SongEntity {
    return SongEntity(
        id = "subsonic_$id", // Prefix to avoid conflicts with YouTube IDs
        title = title,
        duration = duration ?: -1,
        thumbnailUrl = coverArt?.let { "subsonic_cover_$it" }, // Will be resolved by SubsonicRepository
        albumId = albumId?.let { "subsonic_$it" },
        albumName = album,
        explicit = false, // Subsonic doesn't have explicit flag
        year = year,
        date = created?.let { parseSubsonicDate(it) },
        dateModified = null,
        liked = starred != null,
        likedDate = starred?.let { parseSubsonicDate(it) },
        totalPlayTime = (playCount ?: 0) * ((duration ?: 0) * 1000L), // Estimate based on play count
        inLibrary = LocalDateTime.now(), // All Subsonic songs are in library
        dateDownload = null,
        isLocal = false,
        libraryAddToken = null,
        libraryRemoveToken = null,
        romanizeLyrics = true,
        isDownloaded = false,
        isUploaded = false
    )
}

/**
 * Convert Subsonic Album to AlbumEntity
 */
fun Album.toAlbumEntity(): AlbumEntity {
    return AlbumEntity(
        id = "subsonic_$id",
        playlistId = null, // Subsonic albums don't have playlist IDs
        title = name,
        year = year,
        thumbnailUrl = coverArt?.let { "subsonic_cover_$it" },
        themeColor = null,
        songCount = songCount,
        duration = duration,
        explicit = false,
        lastUpdateTime = created?.let { parseSubsonicDate(it) } ?: LocalDateTime.now(),
        bookmarkedAt = starred?.let { parseSubsonicDate(it) },
        likedDate = starred?.let { parseSubsonicDate(it) },
        inLibrary = LocalDateTime.now(),
        isLocal = false,
        isUploaded = false
    )
}

/**
 * Convert Subsonic AlbumWithSongs to AlbumEntity
 */
fun AlbumWithSongs.toAlbumEntity(): AlbumEntity {
    return AlbumEntity(
        id = "subsonic_$id",
        playlistId = null,
        title = name,
        year = year,
        thumbnailUrl = coverArt?.let { "subsonic_cover_$it" },
        themeColor = null,
        songCount = songCount,
        duration = duration,
        explicit = false,
        lastUpdateTime = created?.let { parseSubsonicDate(it) } ?: LocalDateTime.now(),
        bookmarkedAt = null,
        likedDate = null,
        inLibrary = LocalDateTime.now(),
        isLocal = false,
        isUploaded = false
    )
}

/**
 * Convert Subsonic Artist to ArtistEntity
 */
fun Artist.toArtistEntity(): ArtistEntity {
    return ArtistEntity(
        id = "subsonic_$id",
        name = name,
        thumbnailUrl = coverArt?.let { "subsonic_cover_$it" },
        channelId = null, // Subsonic doesn't have channel concept
        lastUpdateTime = LocalDateTime.now(),
        bookmarkedAt = starred?.let { parseSubsonicDate(it) },
        isLocal = false
    )
}

/**
 * Parse Subsonic date string to LocalDateTime
 * Subsonic uses ISO 8601 format: 2012-04-17T19:32:49.000Z
 */
private fun parseSubsonicDate(dateString: String): LocalDateTime {
    return try {
        // Remove the 'Z' and milliseconds for parsing
        val cleanDate = dateString.replace("Z", "").substringBefore(".")
        LocalDateTime.parse(cleanDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    } catch (e: Exception) {
        LocalDateTime.now()
    }
}

/**
 * Extract Subsonic ID from prefixed ID
 */
fun String.toSubsonicId(): String {
    return this.removePrefix("subsonic_")
}

/**
 * Check if ID is from Subsonic
 */
fun String.isSubsonicId(): Boolean {
    return this.startsWith("subsonic_")
}

/**
 * Get cover art ID from thumbnail URL
 */
fun String.getSubsonicCoverArtId(): String? {
    return if (this.startsWith("subsonic_cover_")) {
        this.removePrefix("subsonic_cover_")
    } else {
        null
    }
}
