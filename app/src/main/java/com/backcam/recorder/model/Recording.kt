package com.backcam.recorder.model

data class Recording(
    val id: String,
    val filePath: String,
    val fileName: String,
    val duration: Long,
    val fileSize: Long,
    val resolution: String,
    val frameRate: Int,
    val createdAt: Long,
    val isPrivate: Boolean,
    val thumbnailPath: String?
)