package es.puertosantander.buques.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.puertosantander.buques.data.Buque
import es.puertosantander.buques.data.Lista
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Convierte un código ISO de dos letras en el emoji de su bandera. */
fun banderaEmoji(iso: String): String {
    if (iso.length != 2 || !iso.all { it in 'A'..'Z' }) return ""
    val base = 0x1F1E6 - 'A'.code
    return String(Character.toChars(base + iso[0].code)) +
        String(Character.toChars(base + iso[1].code))
}

/** "en 3 h 20 min", "en 2 d 4 h", "hace 40 min". */
fun tiempoRelativo(momento: LocalDateTime, ahora: LocalDateTime = LocalDateTime.now()): String {
    val d = Duration.between(ahora, momento)
    val pasado = d.isNegative
    val abs = d.abs()
    val dias = abs.toDays()
    val horas = abs.toHours() % 24
    val minutos = abs.toMinutes() % 60
    val texto = when {
        dias > 0 -> "$dias d${if (horas > 0) " $horas h" else ""}"
        abs.toHours() > 0 -> "$horas h${if (minutos > 0) " $minutos min" else ""}"
        else -> "$minutos min"
    }
    return if (pasado) "hace $texto" else "en $texto"
}

/** Muestra "19/09 21:35" y añade el año solo si no es el actual. */
fun fechaCorta(texto: String): String {
    val f = Buque.parseFecha(texto) ?: return texto
    val anioActual = LocalDate.now().year
    val dia = "%02d/%02d".format(f.dayOfMonth, f.monthValue)
    val hora = "%02d:%02d".format(f.hour, f.minute)
    return if (f.year == anioActual) "$dia $hora" else "$dia/${f.year} $hora"
}

@Composable
fun BuqueCard(
    buque: Buque,
    lista: Lista,
    onClick: (Buque) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable { onClick(buque) },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)) {

            // Nombre del buque: al pulsar se abre la ficha de VesselFinder.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (buque.bandera.isNotEmpty()) {
                    Text(banderaEmoji(buque.bandera), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        buque.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (buque.procedencia.isNotEmpty() || buque.bandera.isNotEmpty()) {
                        Text(
                            listOfNotNull(
                                buque.procedencia.ifBlank { null },
                                buque.bandera.ifBlank { null }?.let { "bandera $it" }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Ver ficha en VesselFinder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            if (lista == Lista.EN_PUERTO) {
                SalidaPrevista(buque)
                Spacer(Modifier.height(8.dp))
                DatoFila(Icons.Default.Login, "Atracó", fechaCorta(buque.atraqueInicioTexto))
            } else {
                val etiqueta = if (lista == Lista.ENTRADAS) "Atraque" else "Salida"
                val icono = if (lista == Lista.ENTRADAS) Icons.Default.Login else Icons.Default.Logout
                val valor = if (lista == Lista.ENTRADAS) buque.atraqueInicioTexto else buque.atraqueFinTexto
                DatoFila(icono, etiqueta, fechaCorta(valor), destacado = true)
                val otro = if (lista == Lista.ENTRADAS) buque.atraqueFinTexto else buque.atraqueInicioTexto
                val otraEtiqueta = if (lista == Lista.ENTRADAS) "Salida prevista" else "Atracó"
                if (otro.isNotEmpty()) {
                    DatoFila(Icons.Default.AccessTime, otraEtiqueta, fechaCorta(otro))
                }
            }

            if (buque.muelle.isNotEmpty()) DatoFila(Icons.Default.Anchor, "Muelle", buque.muelle)
            if (buque.consignatario.isNotEmpty()) {
                DatoFila(Icons.Default.Business, "Consignatario", buque.consignatario)
            }
            if (buque.registro.isNotEmpty()) {
                Text(
                    "Escala ${buque.registro}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

/** Bloque destacado con la fecha y hora prevista de salida. */
@Composable
private fun SalidaPrevista(buque: Buque) {
    val fin = buque.atraqueFin
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("SALIDA PREVISTA", style = MaterialTheme.typography.labelSmall)
                Text(
                    if (buque.atraqueFinTexto.isEmpty()) "Sin dato"
                    else fechaCorta(buque.atraqueFinTexto),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (fin != null) {
                Text(
                    tiempoRelativo(fin),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DatoFila(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: String,
    valor: String,
    destacado: Boolean = false
) {
    if (valor.isBlank()) return
    Row(
        Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icono,
            contentDescription = null,
            modifier = Modifier.width(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$etiqueta: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            valor,
            style = if (destacado) MaterialTheme.typography.bodyMedium
            else MaterialTheme.typography.bodySmall,
            fontWeight = if (destacado) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
