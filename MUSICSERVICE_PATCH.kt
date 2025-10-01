// Patch para MusicService.kt - Adicionar suporte Subsonic
// Este arquivo contém as modificações necessárias para integrar Subsonic no MusicService

/*
 * INSTRUÇÕES DE INTEGRAÇÃO:
 * 
 * 1. Adicionar imports no topo do MusicService.kt:
 */

import com.metrolist.music.utils.SubsonicPlayerUtils
import com.metrolist.music.extensions.isSubsonicId

/*
 * 2. Na função createDataSourceFactory(), modificar a parte que obtém a URL de playback:
 * 
 * ANTES (linha ~1290):
 */

// Código original:
songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
    scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
    return@Factory dataSpec.withUri(it.first.toUri())
}

val playbackData = runBlocking(Dispatchers.IO) {
    YTPlayerUtils.playerResponseForPlayback(
        mediaId,
        audioQuality = audioQuality,
        connectivityManager = connectivityManager,
    )
}.getOrElse { throwable ->
    // ... tratamento de erro ...
}

/*
 * DEPOIS (substituir por):
 */

songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
    scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
    return@Factory dataSpec.withUri(it.first.toUri())
}

// Check if this is a Subsonic song
if (mediaId.isSubsonicId()) {
    val streamUrl = runBlocking(Dispatchers.IO) {
        SubsonicPlayerUtils.getStreamUrl(mediaId, this@MusicService)
    } ?: throw PlaybackException(
        getString(R.string.error_unknown),
        null,
        PlaybackException.ERROR_CODE_REMOTE_ERROR
    )
    
    scope.launch(Dispatchers.IO) { 
        // Don't need to recover song data for Subsonic
        // The data is already in the database
    }
    
    return@Factory dataSpec.withUri(streamUrl.toUri())
}

// Continue with YouTube Music logic for non-Subsonic songs
val playbackData = runBlocking(Dispatchers.IO) {
    YTPlayerUtils.playerResponseForPlayback(
        mediaId,
        audioQuality = audioQuality,
        connectivityManager = connectivityManager,
    )
}.getOrElse { throwable ->
    when (throwable) {
        is PlaybackException -> throw throwable

        is java.net.ConnectException, is java.net.UnknownHostException -> {
            throw PlaybackException(
                getString(R.string.error_no_internet),
                throwable,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            )
        }

        is java.net.SocketTimeoutException -> {
            throw PlaybackException(
                getString(R.string.error_timeout),
                throwable,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            )
        }

        else -> throw PlaybackException(
            getString(R.string.error_unknown),
            throwable,
            PlaybackException.ERROR_CODE_REMOTE_ERROR
        )
    }
}

/*
 * 3. Na função recoverSong(), adicionar verificação para Subsonic:
 * 
 * No início da função recoverSong (linha ~770):
 */

private suspend fun recoverSong(
    mediaId: String,
    playbackData: YTPlayerUtils.PlaybackData? = null
) {
    // Skip recovery for Subsonic songs - they're already in database
    if (mediaId.isSubsonicId()) {
        return
    }
    
    // Continue with existing YouTube recovery logic...
    val song = database.song(mediaId).first()
    // ... resto do código ...
}

/*
 * 4. OPCIONAL: Na função onMediaItemTransition(), adicionar scrobbling Subsonic:
 * 
 * Adicionar após a linha que chama scrobbleManager?.onSongStart():
 */

override fun onMediaItemTransition(
    mediaItem: MediaItem?,
    reason: Int,
) {
    // ... código existente ...
    
    if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
        scrobbleManager?.onSongStart(player.currentMetadata, duration = player.duration)
        
        // Scrobble to Subsonic if it's a Subsonic song
        player.currentMetadata?.id?.let { mediaId ->
            if (mediaId.isSubsonicId()) {
                scope.launch {
                    SubsonicPlayerUtils.scrobble(mediaId, submission = false)
                }
            }
        }
    }
    
    // ... resto do código ...
}

/*
 * 5. OPCIONAL: Ao finalizar a música, enviar scrobble final:
 * 
 * Na função onPlaybackStateChanged() ou em onPlayerError():
 */

// Quando a música termina ou pula
if (playbackState == Player.STATE_ENDED || 
    (playbackState == Player.STATE_IDLE && player.currentPosition > player.duration * 0.5)) {
    
    player.currentMetadata?.id?.let { mediaId ->
        if (mediaId.isSubsonicId()) {
            scope.launch {
                SubsonicPlayerUtils.scrobble(mediaId, submission = true)
            }
        }
    }
}

/*
 * RESUMO DAS MUDANÇAS:
 * 
 * ✓ Adicionar imports (SubsonicPlayerUtils, isSubsonicId)
 * ✓ Modificar createDataSourceFactory() para detectar e tratar músicas Subsonic
 * ✓ Modificar recoverSong() para pular músicas Subsonic
 * ✓ OPCIONAL: Adicionar scrobbling Subsonic em onMediaItemTransition()
 * ✓ OPCIONAL: Scrobble final quando música termina
 * 
 * IMPORTANTE:
 * - Estas modificações mantêm a compatibilidade total com YouTube Music
 * - Músicas Subsonic são detectadas pelo prefixo "subsonic_" no ID
 * - O streaming funciona de forma direta via URL HTTP/HTTPS do servidor
 * - Cache e download continuam funcionando normalmente para ambos os tipos
 */
