package es.puertosantander.buques.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import es.puertosantander.buques.data.Buque
import es.puertosantander.buques.data.DetalleBuque

/**
 * Ficha del buque en tres capas:
 *  1. Fotografia obtenida de VesselFinder (si el buque tiene alguna subida).
 *  2. Datos propios: eslora, tonelaje, IMO/MMSI, escala en el puerto.
 *  3. La ficha completa de VesselFinder en un WebView, con su mapa y su AIS.
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaVesselFinder(
    buque: Buque,
    detalle: DetalleBuque?,
    /** Si el buque esta fondeado, el atraque de la misma escala al que ira. */
    atraquePrevisto: Buque? = null,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    var progreso by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var verWeb by remember { mutableStateOf(false) }

    // Con IMO conocido se va directo a la ficha; si no, al buscador por nombre.
    val urlWeb = detalle?.urlFichaImo ?: buque.urlVesselFinder

    BackHandler {
        val wv = webView
        when {
            verWeb && wv != null && wv.canGoBack() -> wv.goBack()
            verWeb -> verWeb = false
            else -> onCerrar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(buque.nombre, fontWeight = FontWeight.SemiBold)
                        Text(
                            detalle?.tipoBuque ?: buque.nombreCompleto,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    if (verWeb) {
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                        }
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlWeb)))
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

            if (!verWeb) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FotoBuque(detalle)
                    AvisoCoincidencia(detalle)
                    DatosBuque(buque, detalle, atraquePrevisto)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .clickable { verWeb = true }
                    ) {
                        Row(
                            Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Ver posición, mapa y datos AIS en VesselFinder",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                        }
                    }
                }
            } else {
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
                            loadUrl(urlWeb)
                            webView = this
                        }
                    }
                )
            }
        }
    }
}

/**
 * Advierte cuando la ficha de VesselFinder se ha localizado solo por el nombre.
 * Hay homonimos de tamanos muy distintos (el ferry SALAMANCA y varios veleros),
 * asi que si no se ha podido comprobar la eslora conviene decirlo.
 */
@Composable
private fun AvisoCoincidencia(detalle: DetalleBuque?) {
    if (detalle == null || !detalle.vesselFinderConsultado) return
    if (detalle.imo == null) return

    val verificada = detalle.coincidenciaVerificada
    Surface(
        color = if (verificada) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (verificada) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
    ) {
        Text(
            if (verificada)
                "Identificación verificada: la eslora coincide con la del puerto" +
                    (detalle.diferenciaEsloraM?.let { String.format(java.util.Locale("es", "ES"), " (%.1f m de diferencia)", it) } ?: "")
            else
                "Atención: ficha localizada solo por el nombre, sin poder comprobar la eslora. " +
                    "Puede corresponder a otro buque homónimo.",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(10.dp)
        )
    }
}

/** Fotografia del buque servida por VesselFinder, con marcador de posicion. */
@Composable
private fun FotoBuque(detalle: DetalleBuque?) {
    val url = detalle?.urlFoto
    Box(
        Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            url != null -> SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .setHeader("Referer", "https://www.vesselfinder.com/")
                    .crossfade(true)
                    .build(),
                contentDescription = "Fotografía del buque",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { CircularProgressIndicator(strokeWidth = 2.dp) },
                error = { SinFoto("No se ha podido cargar la fotografía") }
            )

            detalle?.vesselFinderConsultado == true ->
                SinFoto("Sin fotografía disponible en VesselFinder")

            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Buscando la ficha del buque…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SinFoto(mensaje: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.DirectionsBoat,
                contentDescription = null,
                modifier = Modifier.height(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                mensaje,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Datos tecnicos y de la escala, en dos columnas. */
@Composable
private fun DatosBuque(buque: Buque, detalle: DetalleBuque?, atraquePrevisto: Buque?) {
    val filas = buildList {
        esloraTexto(detalle)?.let { add("Eslora" to it) }
        detalle?.mangaM?.let { add("Manga" to String.format(java.util.Locale("es", "ES"), "%.1f m", it)) }
        tonelajeTexto(detalle)?.let { add("Arqueo bruto" to it) }
        detalle?.tipoBuque?.let { add("Tipo" to it) }
        detalle?.anioConstruccion?.let { add("Construido" to it) }
        detalle?.imo?.let { add("IMO" to it) }
        detalle?.mmsi?.let { add("MMSI" to it) }
        if (buque.bandera.isNotEmpty()) {
            add("Bandera" to "${banderaEmoji(buque.bandera)} ${buque.bandera}")
        }
        if (buque.esFondeo) {
            add("Situación" to "Fondeado, fuera de la bahía")
            add(
                "Atraque previsto" to (
                    atraquePrevisto?.let { "${it.muelle} · ${it.atraqueInicioTexto}" }
                        ?: "Sin asignar todavía"
                    )
            )
            if (buque.atraqueInicioTexto.isNotEmpty()) add("Fondea" to buque.atraqueInicioTexto)
            if (buque.atraqueFinTexto.isNotEmpty()) add("Leva anclas" to buque.atraqueFinTexto)
        } else {
            if (buque.muelle.isNotEmpty()) add("Muelle" to buque.muelle)
            if (buque.atraqueInicioTexto.isNotEmpty()) add("Atraque" to buque.atraqueInicioTexto)
            if (buque.atraqueFinTexto.isNotEmpty()) add("Salida prevista" to buque.atraqueFinTexto)
        }
        buque.procedencia.takeIf { it.isNotEmpty() }?.let { add("Procedencia" to it) }
        detalle?.destino?.let { add("Destino" to it) }
        detalle?.tipoMercancia?.let { add("Mercancía" to it) }
        if (buque.consignatario.isNotEmpty()) add("Consignatario" to buque.consignatario)
        if (buque.registro.isNotEmpty()) add("Escala" to buque.registro)
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        filas.forEach { (etiqueta, valor) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    etiqueta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(120.dp)
                )
                Text(
                    valor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (etiqueta in setOf("Eslora", "Salida prevista", "Atraque previsto"))
                        FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
