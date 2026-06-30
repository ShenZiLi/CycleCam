package com.backcam.recorder.model

sealed class RecordState {
    object Idle : RecordState()
    object Preparing : RecordState()
    data class Recording(val duration: Long) : RecordState()
    object Paused : RecordState()
    object Stopping : RecordState()
    data class Error(val message: String) : RecordState()
}