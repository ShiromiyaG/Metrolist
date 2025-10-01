package com.metrolist.subsonic

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import com.metrolist.subsonic.models.*

/**
 * Subsonic API Client
 * Compatible with Subsonic API 1.16.1 and OpenSubsonic
 * Works with Navidrome, Subsonic, Airsonic, and other compatible servers
 */
object Subsonic {
    private const val CLIENT_NAME = "Metrolist"
    private const val API_VERSION = "1.16.1"
    
    var serverUrl: String = ""
    var username: String = ""
    var password: String = ""
    
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
    }
    
    /**
     * Generate MD5 hash for authentication
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }
    
    /**
     * Generate random salt for authentication
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Build base URL with authentication parameters
     */
    private fun buildUrl(endpoint: String): String {
        val salt = generateSalt()
        val token = md5(password + salt)
        return "$serverUrl/rest/$endpoint"
    }
    
    /**
     * Get authentication parameters
     */
    private fun getAuthParams(): Map<String, String> {
        val salt = generateSalt()
        val token = md5(password + salt)
        return mapOf(
            "u" to username,
            "t" to token,
            "s" to salt,
            "v" to API_VERSION,
            "c" to CLIENT_NAME,
            "f" to "json"
        )
    }
    
    /**
     * Ping the server to test connectivity
     */
    suspend fun ping(): Result<Boolean> = runCatching {
        val response = client.get(buildUrl("ping.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
        }.body<SubsonicResponse<Unit>>()
        
        response.subsonicResponse.status == "ok"
    }
    
    /**
     * Search for artists, albums, and songs
     */
    suspend fun search(query: String, artistCount: Int = 20, albumCount: Int = 20, songCount: Int = 50): Result<SearchResult3> = runCatching {
        val response = client.get(buildUrl("search3.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("query", query)
            parameter("artistCount", artistCount)
            parameter("albumCount", albumCount)
            parameter("songCount", songCount)
        }.body<SubsonicResponse<SearchResult3>>()
        
        if (response.subsonicResponse.status == "ok") {
            response.subsonicResponse.data ?: SearchResult3()
        } else {
            throw Exception(response.subsonicResponse.error?.message ?: "Search failed")
        }
    }
    
    /**
     * Get all artists
     */
    suspend fun getArtists(): Result<List<Artist>> = runCatching {
        val response = client.get(buildUrl("getArtists.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
        }.body<SubsonicResponse<ArtistsResponse>>()
        
        if (response.subsonicResponse.status == "ok") {
            response.subsonicResponse.data?.index?.flatMap { it.artists } ?: emptyList()
        } else {
            throw Exception(response.subsonicResponse.error?.message ?: "Failed to get artists")
        }
    }
    
    /**
     * Get artist details with albums
     */
    suspend fun getArtist(artistId: String): Result<Artist> = runCatching {
        val response = client.get(buildUrl("getArtist.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("id", artistId)
        }.body<SubsonicResponse<Artist>>()
        
        if (response.subsonicResponse.status == "ok") {
            response.subsonicResponse.data ?: throw Exception("Artist not found")
        } else {
            throw Exception(response.subsonicResponse.error?.message ?: "Failed to get artist")
        }
    }
    
    /**
     * Get album details with songs
     */
    suspend fun getAlbum(albumId: String): Result<AlbumWithSongs> = runCatching {
        val response = client.get(buildUrl("getAlbum.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("id", albumId)
        }.body<SubsonicResponse<AlbumWithSongs>>()
        
        if (response.subsonicResponse.status == "ok") {
            response.subsonicResponse.data ?: throw Exception("Album not found")
        } else {
            throw Exception(response.subsonicResponse.error?.message ?: "Failed to get album")
        }
    }
    
    /**
     * Get random songs
     */
    suspend fun getRandomSongs(size: Int = 50, genre: String? = null): Result<List<Song>> = runCatching {
        val response = client.get(buildUrl("getRandomSongs.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("size", size)
            genre?.let { parameter("genre", it) }
        }.body<SubsonicResponse<SongsResponse>>()
        
        if (response.subsonicResponse.status == "ok") {
            response.subsonicResponse.data?.songs ?: emptyList()
        } else {
            throw Exception(response.subsonicResponse.error?.message ?: "Failed to get random songs")
        }
    }
    
    /**
     * Get all playlists
     */
    suspend fun getPlaylists(): Result<List<Playlist>> = runCatching {
        val response = client.get(buildUrl("getPlaylists.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
        }.body<SubsonicResponse<PlaylistsResponse>>()
        
        if (response.subsonicResponse.status == "ok") {
            response.subsonicResponse.data?.playlists ?: emptyList()
        } else {
            throw Exception(response.subsonicResponse.error?.message ?: "Failed to get playlists")
        }
    }
    
    /**
     * Get playlist with songs
     */
    suspend fun getPlaylist(playlistId: String): Result<PlaylistWithSongs> = runCatching {
        val response = client.get(buildUrl("getPlaylist.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("id", playlistId)
        }.body<SubsonicResponse<PlaylistWithSongs>>()
        
        if (response.subsonicResponse.status == "ok") {
            response.subsonicResponse.data ?: throw Exception("Playlist not found")
        } else {
            throw Exception(response.subsonicResponse.error?.message ?: "Failed to get playlist")
        }
    }
    
    /**
     * Get recently added albums
     */
    suspend fun getAlbumList(type: String = "recent", size: Int = 50, offset: Int = 0): Result<List<Album>> = runCatching {
        val response = client.get(buildUrl("getAlbumList2.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("type", type)
            parameter("size", size)
            parameter("offset", offset)
        }.body<SubsonicResponse<AlbumsResponse>>()
        
        if (response.subsonicResponse.status == "ok") {
            response.subsonicResponse.data?.albums ?: emptyList()
        } else {
            throw Exception(response.subsonicResponse.error?.message ?: "Failed to get albums")
        }
    }
    
    /**
     * Get stream URL for a song
     */
    fun getStreamUrl(songId: String, maxBitRate: Int? = null): String {
        val authParams = getAuthParams()
        val params = buildString {
            authParams.forEach { (key, value) ->
                if (isNotEmpty()) append("&")
                append("$key=$value")
            }
            append("&id=$songId")
            maxBitRate?.let { append("&maxBitRate=$it") }
        }
        return "$serverUrl/rest/stream.view?$params"
    }
    
    /**
     * Get cover art URL
     */
    fun getCoverArtUrl(coverArtId: String, size: Int? = null): String {
        val authParams = getAuthParams()
        val params = buildString {
            authParams.forEach { (key, value) ->
                if (isNotEmpty()) append("&")
                append("$key=$value")
            }
            append("&id=$coverArtId")
            size?.let { append("&size=$it") }
        }
        return "$serverUrl/rest/getCoverArt.view?$params"
    }
    
    /**
     * Star/favorite a song, album, or artist
     */
    suspend fun star(id: String, albumId: String? = null, artistId: String? = null): Result<Boolean> = runCatching {
        val response = client.get(buildUrl("star.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("id", id)
            albumId?.let { parameter("albumId", it) }
            artistId?.let { parameter("artistId", it) }
        }.body<SubsonicResponse<Unit>>()
        
        response.subsonicResponse.status == "ok"
    }
    
    /**
     * Unstar/unfavorite a song, album, or artist
     */
    suspend fun unstar(id: String, albumId: String? = null, artistId: String? = null): Result<Boolean> = runCatching {
        val response = client.get(buildUrl("unstar.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("id", id)
            albumId?.let { parameter("albumId", it) }
            artistId?.let { parameter("artistId", it) }
        }.body<SubsonicResponse<Unit>>()
        
        response.subsonicResponse.status == "ok"
    }
    
    /**
     * Create a playlist
     */
    suspend fun createPlaylist(name: String, songIds: List<String>? = null): Result<PlaylistWithSongs> = runCatching {
        val response = client.get(buildUrl("createPlaylist.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("name", name)
            songIds?.forEach { songId ->
                parameter("songId", songId)
            }
        }.body<SubsonicResponse<PlaylistWithSongs>>()
        
        if (response.subsonicResponse.status == "ok") {
            response.subsonicResponse.data ?: throw Exception("Failed to create playlist")
        } else {
            throw Exception(response.subsonicResponse.error?.message ?: "Failed to create playlist")
        }
    }
    
    /**
     * Update playlist
     */
    suspend fun updatePlaylist(playlistId: String, name: String? = null, comment: String? = null, 
                               songIdsToAdd: List<String>? = null, songIndexesToRemove: List<Int>? = null): Result<Boolean> = runCatching {
        val response = client.get(buildUrl("updatePlaylist.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("playlistId", playlistId)
            name?.let { parameter("name", it) }
            comment?.let { parameter("comment", it) }
            songIdsToAdd?.forEach { songId ->
                parameter("songIdToAdd", songId)
            }
            songIndexesToRemove?.forEach { index ->
                parameter("songIndexToRemove", index)
            }
        }.body<SubsonicResponse<Unit>>()
        
        response.subsonicResponse.status == "ok"
    }
    
    /**
     * Delete playlist
     */
    suspend fun deletePlaylist(playlistId: String): Result<Boolean> = runCatching {
        val response = client.get(buildUrl("deletePlaylist.view")) {
            getAuthParams().forEach { (key, value) ->
                parameter(key, value)
            }
            parameter("id", playlistId)
        }.body<SubsonicResponse<Unit>>()
        
        response.subsonicResponse.status == "ok"
    }
}
