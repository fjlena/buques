package es.puertosantander.buques.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import es.puertosantander.buques.data.Buque

/**
 * Ficha del buque: datos de la escala según el puerto y, debajo, la ficha de
 * VesselFinder cargada en un WebView (posición, rumbo, datos técnicos, foto).
 *
 * VesselFinder no ofrece API pública gratuita, así que se abre su buscador por
 * nombre de buque; cuando hay una sola coincidencia va directo a la ficha.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaVesselFinder(buque: Buque, onCerrar: () -> Unit) {
    val context = LocalContext.current
    var progreso by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onCerrar()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(buque.nombre, fontWeight = FontWeight.SemiBold)
                        Text("VesselFinder", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                    IconButton(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(buque.urlVesselFinder))
                        )
                    }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Abrir en el navegador")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            ResumenEscala(buque)

            if (progreso in 1..99) {
                LinearProgressIndicator(
                    progress = { progreso / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        webViewClient = WebViewClient()
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progreso = newProgress
                            }
                        }
                        loadUrl(buque.urlVesselFinder)
                        webView = this
                    }
                }
            )
        }
    }
}

/** Datos de la escala que ya tenemos del puerto, sobre la ficha web. */
@Composable
private fun ResumenEscala(buque: Buque) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    buildString {
                        if (buque.bandera.isNotEmpty()) append(banderaEmoji(buque.bandera)).append(' ')
                        append(buque.nombreCompleto)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (buque.muelle.isNotEmpty()) {
                Text("Muelle: ${buque.muelle}", style = MaterialTheme.typography.bodySmall)
            }
            if (buque.atraqueInicioTexto.isNotEmpty()) {
                Text(
                    "Atraque: ${buque.atraqueInicioTexto}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (buque.atraqueFinTexto.isNotEmpty()) {
                Text(
                    "Salida prevista: ${buque.atraqueFinTexto}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (buque.consignatario.isNotEmpty()) {
                Text(
                    "Consignatario: ${buque.consignatario}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
