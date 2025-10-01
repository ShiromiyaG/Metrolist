# 🎨 GUIA VISUAL - Próximos Passos de UI

## 📍 Você Está Aqui

```
┌─────────────────────────────────────────────────────┐
│                  METROLIST APP                      │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ✅ BACKEND (100%)                                  │
│  ├─ ✅ Subsonic API                                │
│  ├─ ✅ SubsonicRepository                          │
│  ├─ ✅ SubsonicExtensions                          │
│  ├─ ✅ SubsonicPlayerUtils                         │
│  └─ ✅ MusicService                                │
│                                                     │
│  ✅ VIEWMODELS (100%)                               │
│  ├─ ✅ HomeViewModel                               │
│  ├─ ✅ OnlineSearchViewModel                       │
│  ├─ ⚠️ AlbumViewModel (próximo)                   │
│  ├─ ⚠️ ArtistViewModel (próximo)                  │
│  └─ ⚠️ PlaylistViewModel (próximo)                │
│                                                     │
│  ⚠️ UI SCREENS (0%) ← VOCÊ ESTÁ AQUI              │
│  ├─ ⚠️ HomeScreen.kt (prioritário!)               │
│  ├─ ⚠️ OnlineSearchScreen.kt (prioritário!)       │
│  ├─ ⚠️ AlbumScreen.kt                             │
│  ├─ ⚠️ ArtistScreen.kt                            │
│  └─ ⚠️ PlaylistScreen.kt                          │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## 🎯 Tarefa #1: Adaptar HomeScreen.kt

### 📁 Localização
```
app/src/main/kotlin/com/metrolist/music/ui/screens/home/HomeScreen.kt
```

### 🔍 O Que Procurar

Procure por algo como:
```kotlin
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // ...
}
```

### ✏️ Mudanças Necessárias

#### 1. Adicionar Observação do Estado Subsonic

**ANTES**:
```kotlin
val quickPicks by viewModel.quickPicks.collectAsState()
```

**DEPOIS**:
```kotlin
val quickPicks by viewModel.quickPicks.collectAsState()
val isSubsonicEnabled by viewModel.isSubsonicEnabled.collectAsState()
```

#### 2. Adicionar Badge Visual

**Adicionar ANTES da seção de Quick Picks**:
```kotlin
// Indicador de fonte
if (isSubsonicEnabled) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudDone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Playing from your Subsonic server",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
```

#### 3. Visualização Lado a Lado

**ANTES da mudança**:
```
┌────────────────────────────────┐
│       Quick Picks              │
├────────────────────────────────┤
│  [Song1] [Song2] [Song3] ...   │
└────────────────────────────────┘
```

**DEPOIS da mudança**:
```
┌────────────────────────────────┐
│  🌥️ Playing from your server   │ ← NOVO
├────────────────────────────────┤
│       Quick Picks              │
├────────────────────────────────┤
│  [Song1] [Song2] [Song3] ...   │
└────────────────────────────────┘
```

---

## 🎯 Tarefa #2: Adaptar OnlineSearchScreen.kt

### 📁 Localização
```
app/src/main/kotlin/com/metrolist/music/ui/screens/search/OnlineSearchScreen.kt
```

### 🔍 O Que Procurar

Procure por algo como:
```kotlin
@Composable
fun OnlineSearchScreen(
    query: String,
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel()
) {
    // ...
}
```

### ✏️ Mudanças Necessárias

#### 1. Adicionar Verificação de Fonte

**LOGO NO INÍCIO do Composable**:
```kotlin
val isSubsonicSearch = viewModel.isSubsonicSearch

if (isSubsonicSearch) {
    // Mostrar resultados Subsonic
    SubsonicSearchResults(
        songs = viewModel.subsonicSongs,
        albums = viewModel.subsonicAlbums,
        artists = viewModel.subsonicArtists,
        onSongClick = { song -> /* navigate */ },
        onAlbumClick = { album -> /* navigate */ },
        onArtistClick = { artist -> /* navigate */ }
    )
} else {
    // Código original do YouTube
    // ...
}
```

#### 2. Criar Composable de Resultados Subsonic

**ADICIONAR novo composable no mesmo arquivo**:
```kotlin
@Composable
private fun SubsonicSearchResults(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Badge
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Results from your Subsonic server",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Songs Section
        if (songs.isNotEmpty()) {
            item {
                Text(
                    text = "Songs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(songs) { song ->
                SongListItem(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }

        // Albums Section
        if (albums.isNotEmpty()) {
            item {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(albums) { album ->
                AlbumListItem(
                    album = album,
                    onClick = { onAlbumClick(album) }
                )
            }
        }

        // Artists Section
        if (artists.isNotEmpty()) {
            item {
                Text(
                    text = "Artists",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(artists) { artist ->
                ArtistListItem(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }

        // Empty State
        if (songs.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

#### 3. Visualização Lado a Lado

**ANTES da mudança** (YouTube):
```
┌────────────────────────────────┐
│  Search: "rock"                │
├────────────────────────────────┤
│  [Top Results]                 │
│  • Song 1 (YouTube)            │
│  • Album 1 (YouTube)           │
│  • Artist 1 (YouTube)          │
└────────────────────────────────┘
```

**DEPOIS da mudança** (Subsonic):
```
┌────────────────────────────────┐
│  Search: "rock"                │
│  🌥️ Results from your server   │ ← NOVO
├────────────────────────────────┤
│  Songs                         │ ← NOVO
│  • Song 1 (Subsonic)           │
│  • Song 2 (Subsonic)           │
│                                │
│  Albums                        │ ← NOVO
│  • Album 1 (Subsonic)          │
│  • Album 2 (Subsonic)          │
│                                │
│  Artists                       │ ← NOVO
│  • Artist 1 (Subsonic)         │
│  • Artist 2 (Subsonic)         │
└────────────────────────────────┘
```

---

## 🛠️ Ferramentas Úteis

### 1. Hot Reload (se disponível)
- Salve o arquivo
- App atualiza automaticamente
- Veja mudanças instantaneamente

### 2. Logs para Debug
Adicione logs para verificar se dados estão chegando:

```kotlin
// No HomeScreen
LaunchedEffect(quickPicks) {
    Log.d("HomeScreen", "Quick picks: ${quickPicks?.size} songs")
    quickPicks?.forEach { song ->
        Log.d("HomeScreen", "Song: ${song.song.title} (ID: ${song.id})")
    }
}

// No OnlineSearchScreen
LaunchedEffect(viewModel.subsonicSongs) {
    Log.d("SearchScreen", "Subsonic songs: ${viewModel.subsonicSongs.size}")
}
```

### 3. Preview do Compose
Adicione previews para testar visualmente:

```kotlin
@Preview(showBackground = true)
@Composable
private fun SubsonicSearchResultsPreview() {
    MetrolistTheme {
        SubsonicSearchResults(
            songs = listOf(
                // Mock data
            ),
            albums = emptyList(),
            artists = emptyList(),
            onSongClick = {},
            onAlbumClick = {},
            onArtistClick = {}
        )
    }
}
```

---

## 📊 Checklist de Verificação

### Antes de Compilar:
- [ ] Imports corretos adicionados
- [ ] Estados observados com `collectAsState()`
- [ ] Verificação de `isSubsonicEnabled` / `isSubsonicSearch`
- [ ] Badges/indicadores visuais adicionados
- [ ] Fallback para código original do YouTube

### Depois de Compilar:
- [ ] App compila sem erros
- [ ] App abre sem crashes
- [ ] Tela Home carrega
- [ ] Quick picks aparecem (YouTube ou Subsonic)
- [ ] Badge aparece quando Subsonic ativo
- [ ] Busca funciona
- [ ] Resultados aparecem corretamente

### Testes Funcionais:
- [ ] Desabilitar Subsonic → Ver dados YouTube
- [ ] Habilitar Subsonic → Ver dados Subsonic
- [ ] Badge muda corretamente
- [ ] Músicas são reproduzidas
- [ ] Navegação funciona

---

## 🐛 Troubleshooting

### Problema: Badge não aparece
**Solução**: Verifique se está observando o estado:
```kotlin
val isSubsonicEnabled by viewModel.isSubsonicEnabled.collectAsState()
```

### Problema: Músicas não aparecem
**Solução**: Verifique logs:
```kotlin
Log.d("HomeScreen", "Quick picks: ${quickPicks?.size}")
```

### Problema: App crasha ao abrir
**Solução**: Verifique se SubsonicRepository está injetado corretamente no ViewModel

### Problema: Resultados vazios
**Solução**: 
1. Verificar se Subsonic está habilitado nas configurações
2. Verificar se servidor está acessível
3. Verificar logs do SubsonicRepository

---

## 🎨 Dicas de UI/UX

### 1. Loading States
Adicione indicadores de carregamento:
```kotlin
if (quickPicks == null) {
    CircularProgressIndicator()
} else {
    // Show content
}
```

### 2. Empty States
Mostre mensagem quando não há dados:
```kotlin
if (quickPicks.isEmpty()) {
    Text("No songs available")
} else {
    // Show songs
}
```

### 3. Error States
Mostre erros ao usuário:
```kotlin
val errorMessage by viewModel.errorMessage.collectAsState()

errorMessage?.let { error ->
    Snackbar(
        action = {
            TextButton(onClick = { viewModel.retry() }) {
                Text("Retry")
            }
        }
    ) {
        Text(error)
    }
}
```

---

## 🚀 Depois de Completar

### ✅ Tarefas Básicas Completas:
1. HomeScreen adaptado
2. OnlineSearchScreen adaptado
3. App compila e roda
4. Subsonic funcional

### 🎯 Próximos Passos Avançados:
1. AlbumViewModel + AlbumScreen
2. ArtistViewModel + ArtistScreen
3. PlaylistViewModel + PlaylistScreen
4. Sincronização automática
5. Download offline

---

## 📚 Arquivos de Referência

- `SESSAO_VIEWMODELS.md` - Resumo completo desta sessão
- `VIEWMODELS_INTEGRATION.md` - Detalhes técnicos de integração
- `DEV_GUIDE.md` - Guia geral para desenvolvedores
- `SUBSONIC_MIGRATION.md` - Visão geral da migração

---

**🎯 Foco:** Adapte HomeScreen.kt primeiro. É a tela mais visível e mais fácil de testar!

**⏱️ Tempo estimado:** 30-60 minutos por tela

**💪 Você consegue!** Os ViewModels já estão prontos, só precisa exibir os dados!

---

*Guia criado em 2025-10-01*
