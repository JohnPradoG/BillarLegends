package com.billarlegends.pdfreader.pdf

sealed interface PdfUiState {
    data object Empty : PdfUiState
    data object Loading : PdfUiState
    data class Error(val message: String) : PdfUiState
    data class Loaded(
        val fileName: String,
        val pageCount: Int
    ) : PdfUiState
}
