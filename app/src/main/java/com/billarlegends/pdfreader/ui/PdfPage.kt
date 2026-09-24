package com.billarlegends.pdfreader.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.billarlegends.pdfreader.pdf.PdfViewModel

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

/**
 * Renders a single PDF page at the width of the container and supports pinch-to-zoom
 * plus panning. [onZoomChanged] reports whether the page is currently zoomed in so the
 * parent pager can disable horizontal swipe-to-change-page while the user is panning.
 */
@Composable
fun PdfPage(
    viewModel: PdfViewModel,
    pageIndex: Int,
    onZoomChanged: (Boolean) -> Unit
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = with(density) { maxWidth.toPx() }.toInt()
        var bitmap by remember(pageIndex, widthPx) { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(pageIndex, widthPx) {
            bitmap = null
            bitmap = viewModel.renderPage(pageIndex, widthPx)
        }

        val currentBitmap = bitmap
        if (currentBitmap == null) {
            CircularProgressIndicator()
        } else {
            var scale by remember(pageIndex) { mutableFloatStateOf(1f) }
            var offsetX by remember(pageIndex) { mutableFloatStateOf(0f) }
            var offsetY by remember(pageIndex) { mutableFloatStateOf(0f) }

            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Página ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(pageIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            scale = newScale
                            offsetX = if (newScale > MIN_SCALE) offsetX + pan.x else 0f
                            offsetY = if (newScale > MIN_SCALE) offsetY + pan.y else 0f
                            onZoomChanged(newScale > MIN_SCALE + 0.01f)
                        }
                    }
                    .pointerInput(pageIndex) {
                        detectTapGestures(
                            onDoubleTap = {
                                val zoomedIn = scale <= MIN_SCALE + 0.01f
                                scale = if (zoomedIn) 2.5f else MIN_SCALE
                                offsetX = 0f
                                offsetY = 0f
                                onZoomChanged(zoomedIn)
                            }
                        )
                    }
            )
        }
    }
}
