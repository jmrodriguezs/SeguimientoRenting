package com.manursan.seguimientokm.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.manursan.seguimientokm.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Muestra una foto guardada por la app, decodificada en segundo plano al tamaño pedido. */
@Composable
fun PhotoImage(name: String, modifier: Modifier = Modifier, maxPx: Int = 400, contentScale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, name, maxPx) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val f = Photos.file(context, name)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.path, bounds)
                var sample = 1
                while (bounds.outWidth / sample > maxPx * 2 || bounds.outHeight / sample > maxPx * 2) sample *= 2
                BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) Image(b.asImageBitmap(), contentDescription = "Foto del cuentakilómetros", modifier = Modifier.fillMaxSize(), contentScale = contentScale)
        else CircularProgressIndicator()
    }
}
