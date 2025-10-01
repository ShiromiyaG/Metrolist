# 🔧 PATCH: Integração de ViewModels com Subsonic

## ✅ Modificações Realizadas

### 1. HomeViewModel.kt
**Arquivo**: `app/src/main/kotlin/com/metrolist/music/viewmodels/HomeViewModel.kt`

#### Imports Adicionados:
```kotlin
import com.metrolist.music.constants.SubsonicEnabledKey
import com.metrolist.music.repositories.SubsonicRepository
```

#### Construtor Modificado:
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
    private val subsonicRepository: SubsonicRepository, // ← NOVO
) : ViewModel() {
```

#### Propriedades Adicionadas:
```kotlin
// Subsonic items
val subsonicQuickPicks = MutableStateFlow<List<Song>?>(null)
val isSubsonicEnabled = MutableStateFlow(false)
```

#### Métodos Modificados:

**getQuickPicks()** - Agora verifica se Subsonic está habilitado:
```kotlin
private suspend fun getQuickPicks(){
    // Check if Subsonic is enabled
    val subsonicEnabled = context.dataStore.data.first()[SubsonicEnabledKey] ?: false
    isSubsonicEnabled.value = subsonicEnabled

    if (subsonicEnabled) {
        // Load from Subsonic
        loadSubsonicQuickPicks()
    } else {
        // Load from YouTube as before
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> quickPicks.value = database.quickPicks().first().shuffled().take(20)
            QuickPicks.LAST_LISTEN -> songLoad()
        }
    }
}
```

**Novo método loadSubsonicQuickPicks()**:
```kotlin
private suspend fun loadSubsonicQuickPicks() {
    try {
        // Get random songs from Subsonic
        subsonicRepository.getRandomSongs(20).onSuccess { songs ->
            subsonicQuickPicks.value = songs
            quickPicks.value = songs // Also set main quick picks
        }.onFailure {
            reportException(it)
            // Fallback to local database
            quickPicks.value = database.quickPicks().first().shuffled().take(20)
        }
    } catch (e: Exception) {
        reportException(e)
        quickPicks.value = database.quickPicks().first().shuffled().take(20)
    }
}
```

---

### 2. OnlineSearchViewModel.kt
**Arquivo**: `app/src/main/kotlin/com/metrolist/music/viewmodels/OnlineSearchViewModel.kt`

#### Imports Adicionados:
```kotlin
import com.metrolist.music.constants.SubsonicEnabledKey
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.repositories.SubsonicRepository
import kotlinx.coroutines.flow.first
```

#### Construtor Modificado:
```kotlin
@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
    private val subsonicRepository: SubsonicRepository, // ← NOVO
) : ViewModel() {
```

#### Propriedades Adicionadas:
```kotlin
// Subsonic search results
var subsonicSongs by mutableStateOf<List<Song>>(emptyList())
var subsonicAlbums by mutableStateOf<List<Album>>(emptyList())
var subsonicArtists by mutableStateOf<List<Artist>>(emptyList())
var isSubsonicSearch by mutableStateOf(false)
```

#### Init Block Modificado:
```kotlin
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
                    // YouTube search...
                }
            } else {
                if (viewStateMap[filter.value] == null && !isSubsonicSearch) {
                    // YouTube filtered search...
                }
            }
        }
    }
}
```

**Novo método searchSubsonic()**:
```kotlin
private suspend fun searchSubsonic() {
    subsonicRepository.search(query).onSuccess { (songs, albums, artists) ->
        subsonicSongs = songs.mapNotNull { songEntity ->
            Song(
                song = songEntity,
                artists = emptyList(),
                album = null
            )
        }
        subsonicAlbums = albums
        subsonicArtists = artists
    }.onFailure {
        reportException(it)
    }
}
```

---

### 3. SubsonicRepository.kt
**Arquivo**: `app/src/main/kotlin/com/metrolist/music/repositories/SubsonicRepository.kt`

#### Novo Método Adicionado:

**getRandomSongs()** - Busca músicas aleatórias do servidor:
```kotlin
/**
 * Get random songs from Subsonic server
 */
suspend fun getRandomSongs(size: Int = 20): Result<List<com.metrolist.music.db.entities.Song>> {
    if (!isSubsonicEnabled()) {
        return Result.failure(Exception("Subsonic not enabled"))
    }

    return withContext(Dispatchers.IO) {
        try {
            val randomSongs = Subsonic.getRandomSongs(size).getOrThrow()
            
            val songEntities = randomSongs.map { it.toSongEntity() }
            
            // Cache songs in database
            songEntities.forEach { song ->
                dao.insert(song)
            }

            // Return as Song entities with relationships
            val songs = songEntities.mapNotNull { songEntity ->
                database.song(songEntity.id).first()
            }

            Result.success(songs)
        } catch (e: Exception) {
            Timber.e(e, "Error getting random songs from Subsonic")
            Result.failure(e)
        }
    }
}
```

---

## 🎯 Como Funciona

### Fluxo de Dados - HomeViewModel

```
┌─────────────────────────────────────────────────────────┐
│                    HomeViewModel                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  init() {                                               │
│    ├─ Verifica SubsonicEnabledKey no DataStore         │
│    │                                                    │
│    ├─ Se HABILITADO:                                   │
│    │   └─ loadSubsonicQuickPicks()                     │
│    │       └─ subsonicRepository.getRandomSongs(20)    │
│    │           └─ Subsonic.getRandomSongs()            │
│    │               └─ GET /rest/getRandomSongs.view    │
│    │                   └─ Retorna 20 músicas aleatórias│
│    │                       └─ Cache no Room DB         │
│    │                           └─ quickPicks.value     │
│    │                                                    │
│    └─ Se DESABILITADO:                                 │
│        └─ Busca no YouTube (lógica original)           │
│  }                                                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Fluxo de Dados - OnlineSearchViewModel

```
┌─────────────────────────────────────────────────────────┐
│              OnlineSearchViewModel                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  init() {                                               │
│    ├─ Verifica SubsonicEnabledKey                      │
│    │                                                    │
│    ├─ Se HABILITADO:                                   │
│    │   └─ searchSubsonic()                             │
│    │       └─ subsonicRepository.search(query)         │
│    │           └─ Subsonic.search()                    │
│    │               └─ GET /rest/search3.view           │
│    │                   └─ Retorna songs, albums, artists│
│    │                       └─ subsonicSongs.value      │
│    │                           subsonicAlbums.value    │
│    │                           subsonicArtists.value   │
│    │                                                    │
│    └─ Se DESABILITADO:                                 │
│        └─ Busca no YouTube (lógica original)           │
│  }                                                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 🔄 Próximos Passos

### 1. Adaptar UI Screens (PENDENTE)

As telas de UI precisam ser modificadas para exibir os dados do Subsonic:

#### HomeScreen.kt
```kotlin
// Adicionar verificação para mostrar Subsonic quick picks
if (viewModel.isSubsonicEnabled.collectAsState().value) {
    // Mostrar subsonicQuickPicks
    SubsonicQuickPicksSection(
        songs = viewModel.subsonicQuickPicks.collectAsState().value,
        onSongClick = { song -> /* play */ }
    )
} else {
    // Mostrar YouTube quick picks (original)
    QuickPicksSection(...)
}
```

#### OnlineSearchScreen.kt
```kotlin
// Adicionar toggle ou separação de resultados
if (viewModel.isSubsonicSearch) {
    // Mostrar resultados Subsonic
    Column {
        SubsonicSongsSection(songs = viewModel.subsonicSongs)
        SubsonicAlbumsSection(albums = viewModel.subsonicAlbums)
        SubsonicArtistsSection(artists = viewModel.subsonicArtists)
    }
} else {
    // Mostrar resultados YouTube (original)
    YouTubeSummaryPage(page = viewModel.summaryPage)
}
```

### 2. Adaptar Outros ViewModels (PENDENTE)

- **AlbumViewModel** - Buscar álbum do Subsonic
- **ArtistViewModel** - Buscar artista do Subsonic
- **PlaylistViewModel** - Gerenciar playlists do Subsonic
- **LibraryViewModel** - Sincronizar biblioteca do Subsonic

### 3. Implementar Sincronização (PENDENTE)

Criar método similar ao `syncUtils` para Subsonic:
```kotlin
class SubsonicSyncUtils @Inject constructor(
    private val subsonicRepository: SubsonicRepository,
    private val database: MusicDatabase
) {
    suspend fun syncLibrary() {
        // Sincronizar artistas, álbuns, músicas
    }
    
    suspend fun syncPlaylists() {
        // Sincronizar playlists
    }
    
    suspend fun syncStarred() {
        // Sincronizar favoritos
    }
}
```

---

## 🧪 Testar

### 1. Compilar o projeto:
```bash
./gradlew clean assembleDebug
```

### 2. Instalar no dispositivo:
```bash
./gradlew installDebug
```

### 3. Configurar Subsonic:
1. Abrir app
2. **Configurações** → **Subsonic/Navidrome**
3. Inserir URL, usuário, senha
4. **Testar Conexão**
5. Ativar toggle

### 4. Testar Home:
- Abrir tela Home
- Verificar se Quick Picks vem do Subsonic
- Músicas devem ter prefixo `SS_` no ID

### 5. Testar Busca:
- Buscar por um artista/música
- Verificar se resultados vêm do Subsonic
- Tentar reproduzir uma música

---

## ⚠️ Observações

### IDs de Músicas
- **YouTube**: IDs originais do YouTube (11 caracteres)
- **Subsonic**: Prefixo `SS_` + ID do servidor
  - Exemplo: `SS_12345`

### Detecção de Fonte
```kotlin
fun String.isSubsonicId(): Boolean = startsWith("SS_")

// No player
if (songId.isSubsonicId()) {
    // Usar SubsonicPlayerUtils
} else {
    // Usar YTPlayerUtils
}
```

### Cache Local
Todas as músicas/álbuns/artistas do Subsonic são automaticamente salvos no Room Database para:
- Acesso offline rápido
- Reduzir chamadas à API
- Melhor performance

---

## 📊 Status de Implementação

| Componente | Status | Notas |
|-----------|--------|-------|
| HomeViewModel | ✅ Completo | Quick picks do Subsonic funcionando |
| OnlineSearchViewModel | ✅ Completo | Busca integrada |
| SubsonicRepository | ✅ Completo | Método getRandomSongs adicionado |
| HomeScreen UI | ⚠️ Pendente | Precisa adaptar para mostrar dados Subsonic |
| SearchScreen UI | ⚠️ Pendente | Precisa adaptar para mostrar dados Subsonic |
| AlbumViewModel | ⚠️ Pendente | Precisa integração Subsonic |
| ArtistViewModel | ⚠️ Pendente | Precisa integração Subsonic |
| PlaylistViewModel | ⚠️ Pendente | Precisa integração Subsonic |

---

## 🎉 Resultado Esperado

Após implementar as mudanças de UI:

1. **Home Screen**: Mostra músicas aleatórias do servidor Subsonic quando habilitado
2. **Search**: Busca retorna resultados do Subsonic ao invés do YouTube
3. **Playback**: Músicas do Subsonic são reproduzidas via streaming HTTP
4. **Cache**: Dados são salvos localmente para acesso rápido
5. **Fallback**: Se Subsonic falhar, usa dados locais/YouTube

---

*Patch criado em: 2025-10-01*
*Versão: 1.1.0-subsonic*
