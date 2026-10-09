package com.manursan.seguimientokm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.manursan.seguimientokm.BuildConfig
import com.manursan.seguimientokm.R


/** Diálogo "Acerca de...": logotipo, nombre y versión de la aplicación. */
@Composable
fun AcercaDeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // El logotipo tiene fondo claro: se enmarca igual en modo claro y oscuro
                Box(
                    Modifier.fillMaxWidth().background(Color(0xFFFCFAF8), RoundedCornerShape(16.dp)).padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(R.drawable.logo_jmrsoft), contentDescription = "JMRSoft",
                        contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("Seguimiento Renting", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Versión ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Desarrollada por JMRSoft.",
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
