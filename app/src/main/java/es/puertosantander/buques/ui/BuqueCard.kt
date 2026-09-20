package es.puertosantander.buques.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.puertosantander.buques.data.Buque
import es.puertosantander.buques.data.ClaseMovimiento
import es.puertosantander.buques.data.DetalleBuque
import es.puertosantander.buques.data.Lista
import es.puertosantander.buques.data.Movimiento
import es.puertosantander.buques.data.TipoMovimiento
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Convierte un codigo ISO de dos letras en el emoji de su bandera. */
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

/** Muestra "19/09 21:35" y anade el ano solo si no es el actual. */
fun fechaCorta(texto: String): String {
    val f = Buque.parseFecha(texto) ?: return texto
    val anioActual = LocalDate.now().year
    val dia = "%02d/%02d".format(f.dayOfMonth, f.monthValue)
    val hora = "%02d:%02d".format(f.hour, f.minute)
    return if (f.year == anioActual) "$dia $hora" else "$dia/${f.year} $hora"
}

fun soloHora(texto: String): String {
    val f = Buque.parseFecha(texto) ?: return texto
    return "%02d:%02d".format(f.hour, f.minute)
}

private val ES = java.util.Locale("es", "ES")

/** "140,6 m" a partir de los metros con decimales de la fuente. */
fun esloraTexto(detalle: DetalleBuque?): String? =
    detalle?.esloraM?.let { String.format(ES, "%.1f m", it) }

/** "7.852 GT" con separador de miles espanol. */
fun tonelajeTexto(detalle: DetalleBuque?): String? =
    detalle?.tonelajeBruto?.let { String.format(ES, "%,.0f GT", it) }

// ---------------------------------------------------------------------------
// Tarjeta de movimiento: la usa la pantalla principal
// ---------------------------------------------------------------------------

@Composable
fun MovimientoCard(
    movimiento: Movimiento,
    detalle: DetalleBuque?,
    esProximo: Boolean,
    yaPasado: Boolean,
    /** Muelle al que va el buque fondeado, si se conoce. */
    destinoAtraque: String? = null,
    onClick: (Buque) -> Unit
) {
    val entrada = movimiento.tipo == TipoMovimiento.ENTRADA
    val maniobra = movimiento.clase.esManiobraInterna
    val fondeo = movimiento.clase.esFondeo
    val colorTipo = when {
        // Los cambios de muelle no compiten visualmente con las entradas y
        // salidas reales del puerto: van en gris.
        maniobra -> MaterialTheme.colorScheme.onSurfaceVariant
        fondeo -> MaterialTheme.colorScheme.secondary
        entrada -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (esProximo) 8.dp else 5.dp)
            .clickable { onClick(movimiento.buque) },
        shape = RoundedCornerShape(14.dp),
        border = if (esProximo) BorderStroke(2.dp, colorTipo) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (esProximo) 4.dp else 1.dp)
    ) {
        Column(Modifier.padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)) {

            if (esProximo) {
                Text(
                    "PRÓXIMO MOVIMIENTO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorTipo
                )
                Spacer(Modifier.height(6.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Hora grande a la izquierda: la lista se lee como un horario.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(56.dp)
                ) {
                    Text(
                        soloHora(movimiento.horaTexto),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (yaPasado) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Etiqueta(texto = movimiento.clase.etiqueta, color = colorTipo)
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (movimiento.buque.bandera.isNotEmpty()) {
                            Text(
                                banderaEmoji(movimiento.buque.bandera),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            movimiento.buque.nombre,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val detalles = listOfNotNull(
                        if (fondeo) null else movimiento.buque.muelle.ifBlank { null },
                        esloraTexto(detalle),
                        if (maniobra) null else movimiento.buque.procedencia.ifBlank { null }
                    )
                    if (detalles.isNotEmpty()) {
                        Text(
                            detalles.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (maniobra) {
                        Text(
                            "${movimiento.clase.descripcion} · escala ${movimiento.buque.registro}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (fondeo) {
                        Text(
                            destinoAtraque?.let { "Después atraca en $it" }
                                ?: "Atraque aún sin asignar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    movimiento.momento?.let {
                        Text(
                            tiempoRelativo(it),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (esProximo) FontWeight.Bold else FontWeight.Normal,
                            color = if (esProximo) colorTipo
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Ver ficha del buque",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Tarjeta completa: pestanas de entradas, salidas y buques en el puerto
// ---------------------------------------------------------------------------

@Composable
fun BuqueCard(
    buque: Buque,
    lista: Lista,
    detalle: DetalleBuque?,
    /** Veces que aparece esta misma escala en la lista (atraques del dia). */
    atraquesDeLaEscala: Int = 1,
    /** Muelle al que ira el buque fondeado, si se conoce. */
    destinoAtraque: String? = null,
    /** Se oculta cuando la lista ya viene agrupada por atraque. */
    mostrarMuelle: Boolean = true,
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
                    val subtitulo = listOfNotNull(
                        buque.procedencia.ifBlank { null },
                        detalle?.tipoBuque,
                        buque.bandera.ifBlank { null }?.let { "bandera $it" }
                    )
                    if (subtitulo.isNotEmpty()) {
                        Text(
                            subtitulo.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Ver ficha del buque",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            if (buque.esFondeo) {
                FondeoDestacado(buque, destinoAtraque)
            } else if (lista == Lista.EN_PUERTO) {
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

            esloraTexto(detalle)?.let { eslora ->
                val gt = tonelajeTexto(detalle)
                DatoFila(
                    Icons.Default.Straighten,
                    "Eslora",
                    if (gt != null) "$eslora · $gt" else eslora
                )
            }
            if (mostrarMuelle && !buque.esFondeo && buque.muelle.isNotEmpty()) {
                DatoFila(Icons.Default.Anchor, "Muelle", buque.muelle)
            }
            if (buque.consignatario.isNotEmpty()) {
                DatoFila(Icons.Default.Business, "Consignatario", buque.consignatario)
            }
            if (atraquesDeLaEscala > 1) {
                Text(
                    "Esta escala figura $atraquesDeLaEscala veces hoy: el buque cambia de atraque",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
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

/**
 * Bloque del fondeadero: horas de fondeo y, sobre todo, el atraque al que va
 * el buque, que es lo que interesa saber de un buque que espera fuera.
 */
@Composable
private fun FondeoDestacado(buque: Buque, destinoAtraque: String?) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text("EN FONDEO, FUERA DE LA BAHÍA", style = MaterialTheme.typography.labelSmall)
            Text(
                destinoAtraque?.let { "Atracará en $it" } ?: "Atraque sin asignar todavía",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (buque.atraqueInicioTexto.isNotEmpty()) {
                    Text(
                        "Fondea: ${fechaCorta(buque.atraqueInicioTexto)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (buque.atraqueFinTexto.isNotEmpty()) {
                    Text(
                        "Leva: ${fechaCorta(buque.atraqueFinTexto)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
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
    icono: ImageVector,
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
