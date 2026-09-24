package com.billarlegends.pdfreader.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.billarlegends.pdfreader.pdf.PdfUiState
import com.billarlegends.pdfreader.pdf.PdfViewModel

private val SUPPORTED_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
)

@Composable
fun PdfReaderApp(viewModel: PdfViewModel) {
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importAndOpen(uri)
        }
    }

    val state = viewModel.uiState

    BackHandler(enabled = state !is PdfUiState.Library) {
        viewModel.closeViewer()
    }

    when (state) {
        is PdfUiState.Library -> LibraryScreen(
            state = state,
            onImportClick = { importLauncher.launch(SUPPORTED_MIME_TYPES) },
            onOpenEntry = { viewModel.openEntry(it) },
            onDeleteEntry = { viewModel.deleteEntry(it) },
            onDismissError = { viewModel.dismissError() }
        )

        is PdfUiState.LoadedPdf -> PdfViewerScreen(
            viewModel = viewModel,
            entry = state.entry,
            pageCount = state.pageCount,
            onClose = { viewModel.closeViewer() }
        )

        is PdfUiState.LoadedText -> DocxViewerScreen(
            entry = state.entry,
            content = state.content,
            onClose = { viewModel.closeViewer() }
        )
    }
}
