package com.backcam.recorder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun androidx.navigation.NavGraphBuilder.previewScreen(
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    androidx.navigation.compose.composable("preview") {
        PreviewScreen(
            onNavigateToGallery = onNavigateToGallery,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览区域 (占位)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Text(
                text = "Camera Preview",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 顶部控制栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
            IconButton(onClick = onNavigateToGallery) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = Color.White
                )
            }
        }

        // 底部控制栏
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 录制时长显示
            if (isRecording) {
                Text(
                    text = formatDuration(recordingDuration),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 录制按钮
            FloatingActionButton(
                onClick = { isRecording = !isRecording },
                containerColor = if (isRecording) Color.Red else Color.White,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    tint = if (isRecording) Color.White else Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // 录制指示器
        if (isRecording) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FiberManualRecord,
                    contentDescription = "Recording",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "正在录制",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / 1000 / 60) % 60
    val hours = milliseconds / 1000 / 3600
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}