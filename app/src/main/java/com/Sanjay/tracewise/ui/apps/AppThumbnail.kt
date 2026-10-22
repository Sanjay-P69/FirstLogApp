package com.sanjay.tracewise.ui.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppThumbnailHelper {
    private val iconCache = mutableMapOf<String, ImageBitmap>()

    @Synchronized
    fun getAppIcon(context: Context, packageName: String): ImageBitmap? {
        if (packageName.isBlank()) return null
        iconCache[packageName]?.let { return it }

        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            val imgBitmap = bitmap.asImageBitmap()
            iconCache[packageName] = imgBitmap
            imgBitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = if (drawable.intrinsicWidth >
            0
        ) {
            drawable.intrinsicWidth.coerceIn(48, 144)
        } else {
            96
        }
        val height = if (drawable.intrinsicHeight >
            0
        ) {
            drawable.intrinsicHeight.coerceIn(48, 144)
        } else {
            96
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

@Composable
fun AppThumbnailView(
    packageName: String,
    appName: String,
    style: String = "system_icon",
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(null, packageName) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            AppThumbnailHelper.getAppIcon(context, packageName)
        }
    }
    Surface(
        modifier = modifier.size(size.dp),
        shape = MaterialTheme.shapes.medium,
        color = when (style) {
            "vibrant" -> MaterialTheme.colorScheme.primaryContainer
            "pastel" -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            iconBitmap?.let {
                Image(
                    it,
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp).fillMaxSize()
                )
            }
                ?: Text(
                    appName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleMedium
                )
        }
    }
}
