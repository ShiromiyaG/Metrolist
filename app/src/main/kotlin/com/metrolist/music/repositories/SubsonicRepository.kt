package com.metrolist.music.repositories

import com.metrolist.music.constants.SubsonicEnabledKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.SongAlbumMap
import com.metrolist.music.db.entities.SongArtistMap
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.extensions.getSubsonicCoverArtId
import com.metrolist.music.extensions.isSubsonicId
import com.metrolist.music.extensions.toAlbumEntity
import com.metrolist.music.extensions.toArtistEntity
import com.metrolist.music.extensions.toSongEntity
import com.metrolist.music.extensions.toSubsonicId
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.subsonic.Subsonic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Repository for handling Subsonic/Navidrome data
 */
@Singleton
class SubsonicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase
) {

    /**
     * Check if Subsonic is enabled
     */
    private suspend fun isSubsonicEnabled(): Boolean {
        return context.dataStore.data.first()[SubsonicEnabledKey] ?: false
    }

    /**
     * Search for songs, albums, and artists
     */
    suspend fun search(query: String): Result<Triple<List<SongEntity>, List<AlbumEntity>, List<ArtistEntity>>> {
        if (!isSubsonicEnabled()) {
            return Result.success(Triple(emptyList(), emptyList(), emptyList()))
        }

        return withContext(Dispatchers.IO) {
            try {
                val searchResult = Subsonic.search(query).getOrThrow()
                
                val songs = searchResult.song.map { it.toSongEntity() }
                val albums = searchResult.album.map { it.toAlbumEntity() }
                val artists = searchResult.artist.map { it.toArtistEntity() }

                // Store in database for caching (simplified - skip for now)
                // TODO: Implement proper caching with database.transaction {}

                Result.success(Triple(songs, albums, artists))
            } catch (e: Exception) {
                Timber.e(e, "Error searching Subsonic")
                Result.failure(e)
            }
        }
    }

    /**
     * Get album details with songs
     */
    suspend fun getAlbum(albumId: String): Result<AlbumEntity> {
        if (!isSubsonicEnabled() || !albumId.isSubsonicId()) {
            return Result.failure(Exception("Subsonic not enabled or invalid album ID"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val subsonicAlbumId = albumId.toSubsonicId()
                val albumWithSongs = Subsonic.getAlbum(subsonicAlbumId).getOrThrow()
                
                val albumEntity = albumWithSongs.toAlbumEntity()
                // TODO: Implement proper caching with database.transaction {}

                Result.success(albumEntity)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching Subsonic album")
                Result.failure(e)
            }
        }
    }

    /**
     * Get artist details
     */
    suspend fun getArtist(artistId: String): Result<ArtistEntity> {
        if (!isSubsonicEnabled() || !artistId.isSubsonicId()) {
            return Result.failure(Exception("Subsonic not enabled or invalid artist ID"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val subsonicArtistId = artistId.toSubsonicId()
                val artist = Subsonic.getArtist(subsonicArtistId).getOrThrow()
                
                val artistEntity = artist.toArtistEntity()
                // TODO: Implement proper caching with database.transaction {}

                Result.success(artistEntity)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching Subsonic artist")
                Result.failure(e)
            }
        }
    }

    /**
     * Get random songs
     */
    suspend fun getRandomSongs(count: Int = 50): Result<List<SongEntity>> {
        if (!isSubsonicEnabled()) {
            return Result.success(emptyList())
        }

        return withContext(Dispatchers.IO) {
            try {
                val songs = Subsonic.getRandomSongs(count).getOrThrow()
                val songEntities = songs.map { it.toSongEntity() }

                // TODO: Implement proper caching with database.transaction {}

                Result.success(songEntities)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching random songs")
                Result.failure(e)
            }
        }
    }

    /**
     * Get recent albums
     */
    suspend fun getRecentAlbums(count: Int = 50): Result<List<AlbumEntity>> {
        if (!isSubsonicEnabled()) {
            return Result.success(emptyList())
        }

        return withContext(Dispatchers.IO) {
            try {
                val albums = Subsonic.getAlbumList("recent", count).getOrThrow()
                val albumEntities = albums.map { it.toAlbumEntity() }

                // TODO: Implement proper caching with database.transaction {}

                Result.success(albumEntities)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching recent albums")
                Result.failure(e)
            }
        }
    }

    /**
     * Get all artists
     */
    suspend fun getAllArtists(): Result<List<ArtistEntity>> {
        if (!isSubsonicEnabled()) {
            return Result.success(emptyList())
        }

        return withContext(Dispatchers.IO) {
            try {
                val artists = Subsonic.getArtists().getOrThrow()
                val artistEntities = artists.map { it.toArtistEntity() }

                // TODO: Implement proper caching with database.transaction {}

                Result.success(artistEntities)
            } catch (e: Exception) {
                Timber.e(e, "Error fetching all artists")
                Result.failure(e)
            }
        }
    }

    /**
     * Get stream URL for a song
     */
    fun getStreamUrl(songId: String, maxBitRate: Int? = null): String? {
        if (!songId.isSubsonicId()) return null
        
        val subsonicSongId = songId.toSubsonicId()
        return Subsonic.getStreamUrl(subsonicSongId, maxBitRate)
    }

    /**
     * Get cover art URL
     */
    fun getCoverArtUrl(thumbnailUrl: String?, size: Int? = null): String? {
        val coverArtId = thumbnailUrl?.getSubsonicCoverArtId() ?: return null
        return Subsonic.getCoverArtUrl(coverArtId, size)
    }

    /**
     * Toggle star/favorite for a song
     */
    suspend fun toggleSongStar(songId: String, isStarred: Boolean): Result<Boolean> {
        if (!isSubsonicEnabled() || !songId.isSubsonicId()) {
            return Result.failure(Exception("Subsonic not enabled or invalid song ID"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val subsonicSongId = songId.toSubsonicId()
                val result = if (isStarred) {
                    Subsonic.star(subsonicSongId)
                } else {
                    Subsonic.unstar(subsonicSongId)
                }
                result
            } catch (e: Exception) {
                Timber.e(e, "Error toggling star")
                Result.failure(e)
            }
        }
    }

    /**
     * Toggle star/favorite for an album
     */
    suspend fun toggleAlbumStar(albumId: String, isStarred: Boolean): Result<Boolean> {
        if (!isSubsonicEnabled() || !albumId.isSubsonicId()) {
            return Result.failure(Exception("Subsonic not enabled or invalid album ID"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val subsonicAlbumId = albumId.toSubsonicId()
                val result = if (isStarred) {
                    Subsonic.star("", albumId = subsonicAlbumId)
                } else {
                    Subsonic.unstar("", albumId = subsonicAlbumId)
                }
                result
            } catch (e: Exception) {
                Timber.e(e, "Error toggling album star")
                Result.failure(e)
            }
        }
    }

    /**
     * Toggle star/favorite for an artist
     */
    suspend fun toggleArtistStar(artistId: String, isStarred: Boolean): Result<Boolean> {
        if (!isSubsonicEnabled() || !artistId.isSubsonicId()) {
            return Result.failure(Exception("Subsonic not enabled or invalid artist ID"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val subsonicArtistId = artistId.toSubsonicId()
                val result = if (isStarred) {
                    Subsonic.star("", artistId = subsonicArtistId)
                } else {
                    Subsonic.unstar("", artistId = subsonicArtistId)
                }
                result
            } catch (e: Exception) {
                Timber.e(e, "Error toggling artist star")
                Result.failure(e)
            }
        }
    }

    /**
     * Get random songs from Subsonic server for home screen quick picks
     */
    suspend fun getRandomSongsForHomescreen(size: Int = 20): Result<List<com.metrolist.music.db.entities.Song>> {
        if (!isSubsonicEnabled()) {
            return Result.failure(Exception("Subsonic not enabled"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val randomSongs = Subsonic.getRandomSongs(size).getOrThrow()
                
                val songEntities = randomSongs.map { it.toSongEntity() }
                
                // TODO: Implement proper caching with database.transaction {}

                // Return as Song entities with relationships (simplified for now)
                val songs = songEntities.map { songEntity ->
                    com.metrolist.music.db.entities.Song(
                        song = songEntity,
                        artists = emptyList(),
                        album = null
                    )
                }

                Result.success(songs)
            } catch (e: Exception) {
                Timber.e(e, "Error getting random songs from Subsonic")
                Result.failure(e)
            }
        }
    }
}
