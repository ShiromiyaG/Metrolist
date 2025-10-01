# Guia Rápido para Desenvolvedores - Subsonic Integration

## 🚀 Start rápido

### 1. Entender a arquitetura

```
Subsonic Server → Subsonic.kt → SubsonicRepository → Room DB → ViewModels → UI
```

### 2. Arquivos principais

| Arquivo | Propósito | Status |
|---------|-----------|--------|
| `subsonic/src/main/kotlin/com/metrolist/subsonic/Subsonic.kt` | API Client | ✅ Completo |
| `SubsonicRepository.kt` | Data Layer | ✅ Completo |
| `SubsonicPlayerUtils.kt` | Streaming | ✅ Completo |
| `SubsonicExtensions.kt` | Mappers | ✅ Completo |
| `SubsonicSettings.kt` | UI Config | ✅ Completo |
| `MusicService.kt` | Player | ✅ Modificado |

---

## 🔧 Como Adicionar Subsonic a um ViewModel

### Exemplo: SearchViewModel

```kotlin
// 1. Injetar SubsonicRepository
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val subsonicRepository: SubsonicRepository, // ADICIONAR
) : ViewModel() {

    // 2. Adicionar função de busca Subsonic
    fun searchSubsonic(query: String) = viewModelScope.launch {
        val result = subsonicRepository.search(query)
        
        result.onSuccess { (songs, albums, artists) ->
            // Processar resultados
            _searchResults.value = SearchResults(
                songs = songs,
                albums = albums,
                artists = artists
            )
        }.onFailure { error ->
            // Tratar erro
            Timber.e(error, "Subsonic search failed")
        }
    }

    // 3. Combinar com busca YouTube (opcional)
    fun searchAll(query: String) = viewModelScope.launch {
        // Busca YouTube
        val ytResults = searchYouTube(query)
        
        // Busca Subsonic
        val subsonicResults = subsonicRepository.search(query).getOrNull()
        
        // Combinar resultados
        val combined = ytResults + (subsonicResults?.let {
            Triple(it.first, it.second, it.third)
        } ?: Triple(emptyList(), emptyList(), emptyList()))
        
        _searchResults.value = combined
    }
}
```

---

## 🎵 Como Reproduzir Música Subsonic

### O player já está configurado! Apenas use:

```kotlin
// Em qualquer lugar onde você reproduz música
val mediaItem = songEntity.toMediaItem()
player.setMediaItem(mediaItem)
player.prepare()
player.play()

// MusicService detecta automaticamente se é Subsonic e usa SubsonicPlayerUtils
```

### Como funciona?

1. SongEntity tem ID com prefixo `subsonic_`
2. MusicService detecta o prefixo via `isSubsonicId()`
3. Usa `SubsonicPlayerUtils.getStreamUrl()` ao invés de YouTube
4. ExoPlayer reproduz normalmente

---

## 🖼️ Como Carregar Cover Art Subsonic

### Opção 1: Usar SubsonicRepository

```kotlin
val coverUrl = subsonicRepository.getCoverArtUrl(
    thumbnailUrl = song.thumbnailUrl,
    size = 500
)

// Usar com Coil
AsyncImage(
    model = coverUrl,
    contentDescription = "Album art"
)
```

### Opção 2: Direto do Subsonic client

```kotlin
val coverUrl = Subsonic.getCoverArtUrl(
    coverArtId = "subsonic_cover_123",
    size = 500
)
```

---

## 🔄 Como Sincronizar Dados

### Sincronizar Álbuns Recentes

```kotlin
viewModelScope.launch {
    val albums = subsonicRepository.getRecentAlbums(count = 50)
    
    albums.onSuccess { albumList ->
        // Albums já estão no banco de dados Room
        // Apenas buscar do banco
        val fromDb = database.albumsFlow().first()
        _albums.value = fromDb
    }
}
```

### Sincronizar Biblioteca Completa

```kotlin
suspend fun syncLibrary() {
    // 1. Buscar artistas
    val artists = subsonicRepository.getAllArtists().getOrNull()
    
    // 2. Para cada artista, buscar álbuns
    artists?.forEach { artist ->
        // Implementar lógica de busca de álbuns
    }
    
    // 3. Atualizar UI
    _syncStatus.value = SyncStatus.COMPLETE
}
```

---

## ⭐ Como Implementar Favoritos

### Já está funcionando! Apenas use:

```kotlin
// Em um botão de favorito
viewModelScope.launch {
    val isStarred = !song.liked
    
    // Atualizar no banco local
    database.query {
        update(song.copy(
            liked = isStarred,
            likedDate = if (isStarred) LocalDateTime.now() else null
        ))
    }
    
    // Sincronizar com servidor Subsonic
    subsonicRepository.toggleSongStar(song.id, isStarred)
}
```

---

## 🔍 Como Verificar Se É Subsonic

```kotlin
import com.metrolist.music.extensions.isSubsonicId

// Verificar ID
if (songId.isSubsonicId()) {
    // É do Subsonic
    val subsonicId = songId.toSubsonicId() // Remove prefixo
} else {
    // É do YouTube
}

// Verificar configuração
if (SubsonicPlayerUtils.isConfigured()) {
    // Subsonic está configurado
}
```

---

## 🛠️ Debugging

### Logs úteis

```kotlin
import timber.log.Timber

// Ver configuração
Timber.d("Subsonic URL: ${Subsonic.serverUrl}")
Timber.d("Username: ${Subsonic.username}")

// Ver stream URL
val url = SubsonicPlayerUtils.getStreamUrl(songId, context)
Timber.d("Stream URL: $url")

// Testar conexão
val ping = Subsonic.ping()
Timber.d("Ping result: $ping")
```

### Testar sem servidor

```kotlin
// Usar servidor demo público
Subsonic.serverUrl = "https://demo.navidrome.org"
Subsonic.username = "demo"
Subsonic.password = "demo"

val result = Subsonic.ping()
// deve retornar Success(true)
```

---

## 📝 Checklist de Implementação

### Para adicionar Subsonic a uma nova tela:

- [ ] Injetar `SubsonicRepository` no ViewModel
- [ ] Adicionar verificação `if (isSubsonicEnabled())`
- [ ] Implementar busca/carregamento de dados Subsonic
- [ ] Combinar com dados YouTube (se necessário)
- [ ] Adicionar indicador visual (badge "Subsonic")
- [ ] Tratar erros de conexão
- [ ] Testar com servidor real

---

## ⚠️ Cuidados Importantes

### 1. Sempre verificar se está habilitado

```kotlin
if (context.dataStore.get(SubsonicEnabledKey, false)) {
    // Usar Subsonic
}
```

### 2. IDs têm prefixo

```kotlin
// ❌ ERRADO
Subsonic.getAlbum("123")

// ✅ CORRETO
val albumId = "subsonic_123"
val subsonicId = albumId.toSubsonicId() // "123"
Subsonic.getAlbum(subsonicId)
```

### 3. Tratar timeouts

```kotlin
try {
    val result = withTimeout(10.seconds) {
        subsonicRepository.search(query)
    }
} catch (e: TimeoutCancellationException) {
    // Servidor lento ou offline
}
```

---

## 🎯 Exemplos Completos

### Exemplo 1: Tela de Álbum

```kotlin
@Composable
fun AlbumScreen(
    albumId: String,
    viewModel: AlbumViewModel = hiltViewModel()
) {
    val album by viewModel.album.collectAsState()
    
    LaunchedEffect(albumId) {
        if (albumId.isSubsonicId()) {
            viewModel.loadSubsonicAlbum(albumId)
        } else {
            viewModel.loadYouTubeAlbum(albumId)
        }
    }
    
    // UI normal
}
```

### Exemplo 2: Botão de Play

```kotlin
@Composable
fun PlayButton(song: SongEntity) {
    val player = LocalPlayer.current
    
    IconButton(
        onClick = {
            // Funciona tanto para YouTube quanto Subsonic!
            player.setMediaItem(song.toMediaItem())
            player.prepare()
            player.play()
        }
    ) {
        Icon(Icons.Default.PlayArrow, "Play")
    }
}
```

### Exemplo 3: Badge de Fonte

```kotlin
@Composable
fun SongItem(song: SongEntity) {
    Row {
        // ... conteúdo ...
        
        if (song.id.isSubsonicId()) {
            Badge { Text("Subsonic") }
        } else {
            Badge { Text("YouTube") }
        }
    }
}
```

---

## 🚀 Performance Tips

### 1. Cache de Cover Arts

```kotlin
// Coil já faz cache automático, mas você pode configurar:
val imageLoader = ImageLoader.Builder(context)
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("subsonic_images"))
            .maxSizeBytes(100 * 1024 * 1024) // 100 MB
            .build()
    }
    .build()
```

### 2. Paginação

```kotlin
// Para listas grandes
suspend fun loadMore(offset: Int, count: Int = 50) {
    val albums = Subsonic.getAlbumList(
        type = "recent",
        size = count,
        offset = offset
    )
}
```

### 3. Pré-carregamento

```kotlin
// Pré-carregar próxima música
viewModelScope.launch {
    val nextSong = queue.getNext()
    if (nextSong?.id?.isSubsonicId() == true) {
        // Subsonic já retorna URL direta, ExoPlayer faz pré-buffer
        // Nada adicional necessário
    }
}
```

---

## 📚 Referências Rápidas

### Subsonic API Endpoints Usados

| Endpoint | Uso | Implementado |
|----------|-----|--------------|
| `ping.view` | Teste de conexão | ✅ |
| `search3.view` | Busca | ✅ |
| `getArtists.view` | Lista artistas | ✅ |
| `getArtist.view` | Detalhes artista | ✅ |
| `getAlbum.view` | Detalhes álbum | ✅ |
| `getAlbumList2.view` | Lista álbuns | ✅ |
| `getRandomSongs.view` | Músicas aleatórias | ✅ |
| `stream.view` | Streaming | ✅ |
| `getCoverArt.view` | Cover art | ✅ |
| `star.view` | Favoritar | ✅ |
| `unstar.view` | Desfavoritar | ✅ |
| `getPlaylists.view` | Listar playlists | ✅ |
| `getPlaylist.view` | Detalhes playlist | ✅ |
| `createPlaylist.view` | Criar playlist | ✅ |
| `updatePlaylist.view` | Atualizar playlist | ✅ |
| `deletePlaylist.view` | Deletar playlist | ✅ |

### DataStore Keys

```kotlin
SubsonicServerUrlKey       // String
SubsonicUsernameKey        // String
SubsonicPasswordKey        // String
SubsonicEnabledKey         // Boolean
SubsonicMaxBitRateKey      // Int (kbps)
```

---

## ✅ Pronto!

Agora você tem tudo que precisa para trabalhar com Subsonic no Metrolist!

**Dúvidas?** Consulte:
1. `SUBSONIC_MIGRATION.md` - Visão geral
2. `IMPLEMENTACAO_COMPLETA.md` - Status detalhado
3. `MUSICSERVICE_PATCH.kt` - Modificações no player
4. Código-fonte dos arquivos mencionados

**Happy coding! 🎵**
