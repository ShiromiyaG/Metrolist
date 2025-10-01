package com.metrolist.subsonic.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubsonicResponse<T>(
    @SerialName("subsonic-response")
    val subsonicResponse: SubsonicResponseData<T>
)

@Serializable
data class SubsonicResponseData<T>(
    val status: String,
    val version: String,
    val type: String? = null,
    val serverVersion: String? = null,
    val openSubsonic: Boolean? = null,
    val error: SubsonicError? = null,
    val data: T? = null
)

@Serializable
data class SubsonicError(
    val code: Int,
    val message: String
)

// Song/Track model
@Serializable
data class Song(
    val id: String,
    val parent: String? = null,
    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val track: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val size: Long? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val duration: Int? = null,
    val bitRate: Int? = null,
    val path: String? = null,
    val albumId: String? = null,
    val artistId: String? = null,
    val type: String? = null,
    val created: String? = null,
    val starred: String? = null,
    val discNumber: Int? = null,
    val userRating: Int? = null,
    val averageRating: Double? = null,
    val playCount: Long? = null
)

// Album model
@Serializable
data class Album(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val created: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val starred: String? = null,
    val userRating: Int? = null,
    val playCount: Long? = null
)

// Artist model
@Serializable
data class Artist(
    val id: String,
    val name: String,
    val coverArt: String? = null,
    val albumCount: Int = 0,
    val starred: String? = null
)

// Detailed album with songs
@Serializable
data class AlbumWithSongs(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val created: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val song: List<Song> = emptyList()
)

// Playlist model
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    val public: Boolean = false,
    val songCount: Int = 0,
    val duration: Int = 0,
    val created: String? = null,
    val changed: String? = null,
    val coverArt: String? = null
)

// Playlist with songs
@Serializable
data class PlaylistWithSongs(
    val id: String,
    val name: String,
    val comment: String? = null,
    val owner: String? = null,
    val public: Boolean = false,
    val songCount: Int = 0,
    val duration: Int = 0,
    val created: String? = null,
    val changed: String? = null,
    val coverArt: String? = null,
    val entry: List<Song> = emptyList()
)

// Search results
@Serializable
data class SearchResult3(
    val artist: List<Artist> = emptyList(),
    val album: List<Album> = emptyList(),
    val song: List<Song> = emptyList()
)

// API wrapper responses
@Serializable
data class AlbumsResponse(
    @SerialName("album")
    val albums: List<Album> = emptyList()
)

@Serializable
data class ArtistsResponse(
    @SerialName("index")
    val index: List<ArtistIndex> = emptyList()
)

@Serializable
data class ArtistIndex(
    val name: String,
    @SerialName("artist")
    val artists: List<Artist> = emptyList()
)

@Serializable
data class PlaylistsResponse(
    @SerialName("playlist")
    val playlists: List<Playlist> = emptyList()
)

@Serializable
data class SongsResponse(
    @SerialName("song")
    val songs: List<Song> = emptyList()
)
