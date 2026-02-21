package com.offbeats.player

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val viewModel: MusicPlayerViewModel by viewModels()

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onPermissionResult(true)
        } else {
            requestPermission.launch(permission)
        }

        setContent {
            OffBeatsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Palette.Background) {
                    PlayerScreen(viewModel = viewModel, onRequestPermission = {
                        requestPermission.launch(permission)
                    })
                }
            }
        }
    }
}

private object Palette {
    val Accent = Color(0xFFEDAFB8)
    val Light = Color(0xFFF7E1D7)
    val Background = Color(0xFFDEDBD2)
    val Surface = Color(0xFFB0C4B1)
    val Text = Color(0xFF4A5759)
}

@Composable
private fun OffBeatsTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreen(
    viewModel: MusicPlayerViewModel,
    onRequestPermission: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .padding(16.dp)
    ) {
        TopAppBar(
            title = {
                Text("OffBeats", color = Palette.Text, fontWeight = FontWeight.Bold)
            }
        )

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Palette.Text)
                }
            }

            !state.hasPermission -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Storage access is required to play your offline music.", color = Palette.Text)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRequestPermission) { Text("Grant Permission") }
                }
            }

            else -> {
                state.currentSong?.let { song ->
                    NowPlayingCard(
                        song = song,
                        isPlaying = state.isPlaying,
                        progress = state.progress,
                        duration = state.duration,
                        onSeek = viewModel::seekTo,
                        onTogglePlay = viewModel::togglePlay,
                        onNext = viewModel::playNext,
                        onPrev = viewModel::playPrevious
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Library",
                    color = Palette.Text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                    itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
                        SongRow(song = song, isActive = index == state.currentIndex) {
                            viewModel.playAt(index)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startTicker()
    }
}

@Composable
private fun NowPlayingCard(
    song: Song,
    isPlaying: Boolean,
    progress: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.08f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playButtonScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Palette.Light)
                .padding(16.dp)
        ) {
            Text(song.title, color = Palette.Text, fontWeight = FontWeight.Bold)
            Text(song.artist, color = Palette.Text)
            Spacer(Modifier.height(12.dp))

            Slider(
                value = progress.toFloat().coerceAtLeast(0f),
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                onValueChange = { onSeek(it.toLong()) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Palette.Text)
                }
                IconButton(onClick = onTogglePlay, modifier = Modifier.scale(buttonScale)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(40.dp),
                        tint = Palette.Accent
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Palette.Text)
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, isActive: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "songScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(vertical = 4.dp)
            .background(if (isActive) Palette.Surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(song.title, color = Palette.Text, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
            Text(song.artist, color = Palette.Text)
        }
    }
}

fun querySongs(context: Context): List<Song> {
    val songs = mutableListOf<Song>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION
    )

    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        "${MediaStore.Audio.Media.IS_MUSIC} != 0",
        null,
        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            songs.add(
                Song(
                    id = id,
                    title = cursor.getString(titleColumn) ?: "Unknown",
                    artist = cursor.getString(artistColumn) ?: "Unknown Artist",
                    duration = cursor.getLong(durationColumn),
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                )
            )
        }
    }
    return songs
}
