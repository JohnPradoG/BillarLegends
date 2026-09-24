package com.billarlegends.pdfreader.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.billarlegends.pdfreader.pdf.PdfUiState
import com.billarlegends.pdfreader.pdf.PdfViewModel

@Composable
fun PdfReaderApp(viewModel: PdfViewModel) {
    val context = LocalContext.current

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.openDocument(context, uri)
        }
    }

    val state = viewModel.uiState

    BackHandler(enabled = state !is PdfUiState.Empty) {
        viewModel.closeDocument()
    }

    when (state) {
        is PdfUiState.Empty -> HomeScreen(
            onOpenFileClick = { openDocumentLauncher.launch(arrayOf("application/pdf")) }
        )

        is PdfUiState.Loading -> LoadingScreen()

        is PdfUiState.Error -> ErrorScreen(
            message = state.message,
            onRetryClick = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
            onBackClick = { viewModel.closeDocument() }
        )

        is PdfUiState.Loaded -> PdfViewerScreen(
            viewModel = viewModel,
            fileName = state.fileName,
            pageCount = state.pageCount,
            onOpenAnotherFile = { openDocumentLauncher.launch(arrayOf("application/pdf")) },
            onClose = { viewModel.closeDocument() }
        )
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            text = "Abriendo PDF…",
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetryClick,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text("Elegir otro archivo")
        }
        Button(
            onClick = onBackClick,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Volver al inicio")
        }
    }
}
