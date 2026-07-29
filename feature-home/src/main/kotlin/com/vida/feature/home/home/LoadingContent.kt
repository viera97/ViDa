package com.vida.feature.home.home

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Loading state — centered app icon + indeterminate progress indicator.
 *
 * Loads the **actual launcher icon** via [android.content.pm.PackageManager.getApplicationIcon]
 * so it always matches the installed app icon exactly — no hand-rolled vector approximation.
 *
 * Displayed while [com.vida.feature.home.HomeUiState.Loading] is active.
 */
@Composable
fun LoadingContent(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconPainter = remember {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val w = (drawable.intrinsicWidth).coerceAtLeast(1)
        val h = (drawable.intrinsicHeight).coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        BitmapPainter(bitmap.asImageBitmap())
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Actual app icon from the system's PackageManager — guaranteed to be the
            // real launcher icon, no hand-rolled approximation.
            // Size matches the splash_background icon for a seamless transition.
            Image(
                painter = iconPainter,
                contentDescription = "ViDa",
                modifier = Modifier.size(96.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
