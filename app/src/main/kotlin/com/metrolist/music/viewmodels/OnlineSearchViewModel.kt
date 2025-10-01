package com.metrolist.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.SearchSummaryPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.SubsonicEnabledKey
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.models.ItemsPage
import com.metrolist.music.repositories.SubsonicRepository
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
    private val subsonicRepository: SubsonicRepository,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()
    
    // Subsonic search results
    var subsonicSongs by mutableStateOf<List<Song>>(emptyList())
    var subsonicAlbums by mutableStateOf<List<Album>>(emptyList())
    var subsonicArtists by mutableStateOf<List<Artist>>(emptyList())
    var isSubsonicSearch by mutableStateOf(false)

    init {
        viewModelScope.launch {
            // Check if Subsonic is enabled
            isSubsonicSearch = context.dataStore.data.first()[SubsonicEnabledKey] ?: false
            
            if (isSubsonicSearch) {
                // Search via Subsonic
                searchSubsonic()
            }
            
            filter.collect { filter ->
                if (filter == null) {
                    if (summaryPage == null && !isSubsonicSearch) {
                        YouTube
                            .searchSummary(query)
                            .onSuccess {
                                summaryPage =
                                    it.filterExplicit(
                                        context.dataStore.get(
                                            HideExplicitKey,
                                            false,
                                        ),
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else {
                    if (viewStateMap[filter.value] == null && !isSubsonicSearch) {
                        YouTube
                            .search(query, filter)
                            .onSuccess { result ->
                                viewStateMap[filter.value] =
                                    ItemsPage(
                                        result.items
                                            .distinctBy { it.id }
                                            .filterExplicit(
                                                context.dataStore.get(
                                                    HideExplicitKey,
                                                    false
                                                )
                                            ),
                                        result.continuation,
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }
    
    private suspend fun searchSubsonic() {
        subsonicRepository.search(query).onSuccess { (songs, albums, artists) ->
            subsonicSongs = songs.map { songEntity ->
                // Convert SongEntity to Song with relationships
                Song(
                    song = songEntity,
                    artists = emptyList(),
                    album = null
                )
            }
            subsonicAlbums = albums.map { albumEntity ->
                // Convert AlbumEntity to Album
                Album(
                    album = albumEntity,
                    artists = emptyList()
                )
            }
            subsonicArtists = artists.map { artistEntity ->
                // Convert ArtistEntity to Artist
                Artist(
                    artist = artistEntity,
                    songCount = 0,
                    timeListened = 0
                )
            }
        }.onFailure {
            reportException(it)
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + searchResult.items).distinctBy { it.id },
                    searchResult.continuation
                )
            }
        }
    }
}
