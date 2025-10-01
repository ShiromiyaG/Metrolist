package com.metrolist.music.utils

import com.metrolist.music.constants.SubsonicMaxBitRateKey
import com.metrolist.music.extensions.isSubsonicId
import com.metrolist.music.extensions.toSubsonicId
import com.metrolist.subsonic.Subsonic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import android.content.Context

/**
 * Utilities for Subsonic audio playback
 * Similar to YTPlayerUtils but for Subsonic servers
 */
object SubsonicPlayerUtils {
    
    /**
     * Get stream URL for a song
     * @param songId The song ID (with "subsonic_" prefix)
     * @param context Android context to get preferences
     * @return The stream URL or null if not a Subsonic song
     */
    suspend fun getStreamUrl(songId: String, context: Context): String? {
        if (!songId.isSubsonicId()) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val subsonicSongId = songId.toSubsonicId()
                val maxBitRate = context.dataStore.get(SubsonicMaxBitRateKey, 320)
                
                Subsonic.getStreamUrl(subsonicSongId, maxBitRate)
            } catch (e: Exception) {
                Timber.e(e, "Error getting Subsonic stream URL for song: $songId")
                null
            }
        }
    }

    /**
     * Get cover art URL
     * @param coverArtId The cover art ID
     * @param size Optional size for the cover art
     * @return The cover art URL
     */
    fun getCoverArtUrl(coverArtId: String, size: Int? = 500): String {
        return Subsonic.getCoverArtUrl(coverArtId, size)
    }

    /**
     * Check if a song ID is from Subsonic
     */
    fun isSubsonicSong(songId: String): Boolean {
        return songId.isSubsonicId()
    }

    /**
     * Scrobble/report playback to Subsonic server
     * This is used to track play counts and listening history
     */
    suspend fun scrobble(songId: String, submission: Boolean = true): Result<Unit> {
        if (!songId.isSubsonicId()) {
            return Result.failure(Exception("Not a Subsonic song"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val subsonicSongId = songId.toSubsonicId()
                // Subsonic API scrobble endpoint
                // TODO: Implement when needed for accurate play counts
                Timber.d("Scrobbling song: $subsonicSongId, submission: $submission")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error scrobbling to Subsonic")
                Result.failure(e)
            }
        }
    }

    /**
     * Get download URL for offline playback
     * @param songId The song ID
     * @return The download URL
     */
    fun getDownloadUrl(songId: String): String? {
        if (!songId.isSubsonicId()) {
            return null
        }

        val subsonicSongId = songId.toSubsonicId()
        // For Subsonic, download and stream URLs are similar
        // but download URL doesn't do transcoding
        return Subsonic.getStreamUrl(subsonicSongId, maxBitRate = null)
    }

    /**
     * Check if Subsonic is properly configured
     */
    fun isConfigured(): Boolean {
        return Subsonic.serverUrl.isNotEmpty() && 
               Subsonic.username.isNotEmpty() && 
               Subsonic.password.isNotEmpty()
    }

    /**
     * Test connection to Subsonic server
     */
    suspend fun testConnection(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Subsonic.ping()
            } catch (e: Exception) {
                Timber.e(e, "Subsonic connection test failed")
                Result.failure(e)
            }
        }
    }
}
