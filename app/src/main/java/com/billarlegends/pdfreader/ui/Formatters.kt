package com.billarlegends.pdfreader.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.0f KB", kb)
    return String.format(Locale.getDefault(), "%.1f MB", kb / 1024.0)
}

private val dateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es", "ES"))

fun formatAddedDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
