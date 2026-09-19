package es.puertosantander.buques.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Un registro de escala tal y como aparece en las tablas de
 * puertosantander.es (entradas de hoy, salidas de hoy, buques en el puerto).
 */
data class Buque(
    /** Nombre limpio, sin el código de procedencia. Ej. "LUCIA B". */
    val nombre: String,
    /** Código de procedencia/destino entre paréntesis en la web. Ej. "GB/LIV". */
    val procedencia: String,
    /** Nombre tal cual aparece en la web. Ej. "LUCIA B (GB/LIV)". */
    val nombreCompleto: String,
    /** Número de registro de escala. Ej. "1106/2026". */
    val registro: String,
    /** Código ISO del país de bandera. Ej. "PT". */
    val bandera: String,
    /** Muelle o atraque; es la fila de cabecera que agrupa los buques. */
    val muelle: String,
    /** Inicio del atraque, texto original "dd/MM/yyyy - HH:mm". */
    val atraqueInicioTexto: String,
    /** Fin del atraque (salida prevista), texto original. */
    val atraqueFinTexto: String,
    val consignatario: String,
    /** URL de la ficha del buque en la web del puerto. */
    val urlPuerto: String?
) {
    val atraqueInicio: LocalDateTime? get() = parseFecha(atraqueInicioTexto)
    val atraqueFin: LocalDateTime? get() = parseFecha(atraqueFinTexto)

    /** URL de búsqueda de la ficha en VesselFinder por nombre de buque. */
    val urlVesselFinder: String
        get() = "https://www.vesselfinder.com/vessels?name=" +
            java.net.URLEncoder.encode(nombre, "UTF-8")

    companion object {
        private val FORMATO: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")

        fun parseFecha(texto: String): LocalDateTime? = try {
            LocalDateTime.parse(texto.trim(), FORMATO)
        } catch (e: Exception) {
            null
        }
    }
}

enum class Lista(val titulo: String, val url: String) {
    ENTRADAS("Entradas", "https://www.puertosantander.es/es/entradas-hoy"),
    SALIDAS("Salidas", "https://www.puertosantander.es/es/salidas-hoy"),
    EN_PUERTO("En puerto", "https://www.puertosantander.es/es/buques-en-el-puerto")
}

/** Resultado de una descarga: los buques más la fecha de actualización que publica la web. */
data class ResultadoLista(
    val buques: List<Buque> = emptyList(),
    /** Texto de la cabecera H1, p. ej. "Entradas hoy - 19/09/2026 22:15". */
    val encabezado: String? = null
)
