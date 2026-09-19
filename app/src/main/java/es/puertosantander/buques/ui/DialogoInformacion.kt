package es.puertosantander.buques.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Información sobre el origen de los datos. Cumple tres funciones:
 * citar la fuente (que es lo que piden las condiciones de reutilización de
 * información del sector público), dejar claro que la aplicación no es
 * oficial, y dar acceso a los avisos legales de ambas fuentes.
 */
@Composable
fun DialogoInformacion(
    version: String,
    ultimaActualizacion: String?,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val abrir: (String) -> Unit = { url ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar") }
        },
        title = { Text("Origen de los datos") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                LogoPuerto()

                Text(
                    "Autoridad Portuaria de Santander",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Puerto de Santander",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                Text(
                    "Las entradas, salidas, atraques, fondeos y datos de escala se obtienen " +
                        "de las páginas públicas de la Autoridad Portuaria de Santander:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(6.dp))
                Enlace("Entradas de hoy", "https://www.puertosantander.es/es/entradas-hoy", abrir)
                Enlace("Salidas de hoy", "https://www.puertosantander.es/es/salidas-hoy", abrir)
                Enlace("Buques en el puerto", "https://www.puertosantander.es/es/buques-en-el-puerto", abrir)
                Enlace("puertosantander.es", "https://www.puertosantander.es/es", abrir)
                Enlace("Aviso legal del puerto", "https://www.puertosantander.es/es/aviso-legal", abrir)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    "VesselFinder",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "La fotografía del buque, el IMO, el MMSI y los datos AIS proceden de " +
                        "VesselFinder, y se muestran enlazando a su web.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(6.dp))
                Enlace("vesselfinder.com", "https://www.vesselfinder.com/", abrir)
                Enlace("Condiciones de uso", "https://www.vesselfinder.com/terms", abrir)

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Aplicación personal, sin carácter oficial y sin relación con la " +
                            "Autoridad Portuaria de Santander ni con VesselFinder, que no " +
                            "responden de ella. Los nombres y logotipos citados pertenecen a " +
                            "sus titulares y se usan solo para identificar la fuente.\n\n" +
                            "Los datos son informativos y pueden estar incompletos o no " +
                            "actualizados: no deben emplearse para la navegación ni para " +
                            "decisiones operativas. Para información oficial, consulte la web " +
                            "del puerto.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "Versión $version" + (ultimaActualizacion?.let {
                        "\nDatos consultados el $it"
                    } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/**
 * Logotipo del puerto tomado de su propia web, no incluido en la aplicación:
 * así se muestra como atribución a la fuente sin redistribuir la marca. Si no
 * carga, queda el nombre en texto, que es lo que de verdad importa para citar.
 */
@Composable
private fun LogoPuerto() {
    val oscuro = isSystemInDarkTheme()
    val url = if (oscuro)
        "https://www.puertosantander.es/themes/santander/img/logo-puerto-santander-blanco.png"
    else
        "https://www.puertosantander.es/themes/santander/img/logo-puerto-santander-desktop.png"

    AsyncImage(
        model = url,
        contentDescription = "Logotipo del Puerto de Santander",
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(bottom = 10.dp)
    )
}

@Composable
private fun Enlace(texto: String, url: String, abrir: (String) -> Unit) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { abrir(url) }
            .padding(vertical = 4.dp)
    )
}
