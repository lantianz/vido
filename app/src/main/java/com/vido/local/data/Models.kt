package com.vido.local.data

data class VideoItem(
    val uri: String,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val modifiedMs: Long,
    val folder: String,
    val isPrivate: Boolean,
)

data class ScanResult(val discovered: Int, val rejected: Int, val inaccessibleDirectories: Int)
