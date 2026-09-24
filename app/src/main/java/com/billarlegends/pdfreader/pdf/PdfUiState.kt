package com.billarlegends.pdfreader.pdf

sealed interface PdfUiState {
    data class Library(
        val entries: List<LibraryEntry> = emptyList(),
        val isBusy: Boolean = false,
        val errorMessage: String? = null
    ) : PdfUiState

    data class LoadedPdf(
        val entry: LibraryEntry,
        val pageCount: Int
    ) : PdfUiState

    data class LoadedText(
        val entry: LibraryEntry,
        val content: String
    ) : PdfUiState
}
