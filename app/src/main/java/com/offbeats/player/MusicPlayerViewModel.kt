package com.offbeats.player

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: String
)

data class PlayerState(
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val songs: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val progress: Long = 0,
    val duration: Long = 1
) {
    val currentSong: Song?
        get() = songs.getOrNull(currentIndex)
}

class MusicPlayerViewModel(app: Application) : AndroidViewModel(app) {
    private val player = ExoPlayer.Builder(app).build()
    private val _state = MutableStateFlow(PlayerState(isLoading = true))
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            _state.value = _state.value.copy(
                progress = player.currentPosition,
                duration = if (player.duration > 0) player.duration else (_state.value.currentSong?.duration ?: 1)
            )
            handler.postDelayed(this, 400)
        }
    }

    init {
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _state.value = _state.value.copy(currentIndex = player.currentMediaItemIndex)
            }
        })
    }

    fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            _state.value = _state.value.copy(isLoading = false, hasPermission = false)
            return
        }
        val songs = querySongs(getApplication())
        if (songs.isNotEmpty()) {
            player.setMediaItems(songs.map { MediaItem.fromUri(Uri.parse(it.uri)) })
            player.prepare()
        }
        _state.value = PlayerState(
            isLoading = false,
            hasPermission = true,
            songs = songs,
            currentIndex = if (songs.isNotEmpty()) 0 else -1,
            isPlaying = false,
            progress = 0,
            duration = songs.firstOrNull()?.duration ?: 1
        )
    }

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun playAt(index: Int) {
        player.seekToDefaultPosition(index)
        player.play()
        _state.value = _state.value.copy(currentIndex = index, isPlaying = true)
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun playNext() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem() else player.seekToDefaultPosition(0)
        player.play()
    }

    fun playPrevious() {
        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() else player.seekToDefaultPosition(0)
        player.play()
    }

    fun startTicker() {
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onCleared() {
        handler.removeCallbacks(ticker)
        player.release()
        super.onCleared()
    }
}
