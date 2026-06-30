package com.backcam.recorder.model

enum class Resolution(val displayName: String, val width: Int, val height: Int) {
    UHD_4K("4K (2160p)", 3840, 2160),
    FHD_1080P("1080p", 1920, 1080),
    HD_720P("720p", 1280, 720)
}

enum class FrameRate(val displayName: String, val fps: Int) {
    FPS_60("60 fps", 60),
    FPS_30("30 fps", 30)
}

data class AppSettings(
    val resolution: Resolution = Resolution.UHD_4K,
    val frameRate: FrameRate = FrameRate.FPS_60,
    val isPrivacyAlbumEnabled: Boolean = true
)