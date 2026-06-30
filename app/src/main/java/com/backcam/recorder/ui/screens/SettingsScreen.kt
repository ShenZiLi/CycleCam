package com.backcam.recorder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.backcam.recorder.model.*

fun androidx.navigation.NavGraphBuilder.settingsScreen(
    onNavigateBack: () -> Unit
) {
    androidx.navigation.compose.composable("settings") {
        SettingsScreen(onNavigateBack = onNavigateBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var settings by remember {
        mutableStateOf(AppSettings())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // 分辨率设置
        SettingsItem(
            title = "视频分辨率",
            subtitle = settings.resolution.displayName,
            onClick = { /* 显示选择对话框 */ }
        )

        HorizontalDivider()

        // 帧率设置
        SettingsItem(
            title = "视频帧率",
            subtitle = settings.frameRate.displayName,
            onClick = { /* 显示选择对话框 */ }
        )

        HorizontalDivider()

        // 隐私相册开关
        SettingsSwitch(
            title = "隐私相册",
            subtitle = if (settings.isPrivacyAlbumEnabled) {
                "视频保存到隐私相册，系统相册不可见"
            } else {
                "视频直接保存到系统相册"
            },
            checked = settings.isPrivacyAlbumEnabled,
            onCheckedChange = {
                settings = settings.copy(isPrivacyAlbumEnabled = it)
            }
        )

        HorizontalDivider()

        // 存储空间信息
        StorageInfo()
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            IconButton(onClick = onClick) {
                Text(">")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StorageInfo() {
    ListItem(
        headlineContent = { Text("存储空间") },
        supportingContent = { Text("可用空间: 计算中...") }
    )
}