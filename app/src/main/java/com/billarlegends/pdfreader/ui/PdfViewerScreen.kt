package com.billarlegends.pdfreader.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.billarlegends.pdfreader.pdf.PdfViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    viewModel: PdfViewModel,
    fileName: String,
    pageCount: Int,
    onOpenAnotherFile: () -> Unit,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0) { pageCount }
    val coroutineScope = rememberCoroutineScope()
    var pageIsZoomed by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = fileName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Página ${pagerState.currentPage + 1} de $pageCount",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    IconButton(onClick = { showJumpDialog = true }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Ir a página")
                    }
                    IconButton(onClick = onOpenAnotherFile) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Abrir otro PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !pageIsZoomed,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            PdfPage(
                viewModel = viewModel,
                pageIndex = page,
                onZoomChanged = { zoomed -> pageIsZoomed = zoomed }
            )
        }
    }

    if (showJumpDialog) {
        JumpToPageDialog(
            pageCount = pageCount,
            currentPage = pagerState.currentPage,
            onDismiss = { showJumpDialog = false },
            onConfirm = { targetPage ->
                showJumpDialog = false
                coroutineScope.launch { pagerState.scrollToPage(targetPage) }
            }
        )
    }

    LaunchedEffect(pagerState.currentPage) {
        pageIsZoomed = false
    }
}

@Composable
private fun JumpToPageDialog(
    pageCount: Int,
    currentPage: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf((currentPage + 1).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ir a página") },
        text = {
            Column {
                Text("Introduce un número de página entre 1 y $pageCount.")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val target = text.toIntOrNull()
                if (target != null && target in 1..pageCount) {
                    onConfirm(target - 1)
                }
            }) {
                Text("Ir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
