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

    /** Clave con la que se cachean los datos tecnicos: son del buque, no de la escala. */
    val clave: String get() = nombre.uppercase()

    /** Identificador de la escala; agrupa los atraques de una misma estancia. */
    val escala: String get() = registro.ifBlank { clave }

    /**
     * El fondeo no es un atraque: el buque espera fuera de la bahia. La web del
     * puerto lo agrupa bajo el muelle "FONDEO".
     */
    val esFondeo: Boolean
        get() = muelle.uppercase().contains("FONDEO")

    /** Busqueda en VesselFinder por nombre, para cuando no se conoce el IMO. */
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

/**
 * Datos tecnicos del buque. Se completan en dos fases y de dos fuentes: la
 * ficha del puerto (eslora, tonelaje, destino) y VesselFinder (foto, IMO, MMSI,
 * tipo). Cada campo es opcional: puede fallar una fuente sin perder la otra.
 */
data class DetalleBuque(
    val esloraM: Double? = null,
    val mangaM: Double? = null,
    val tonelajeBruto: Double? = null,
    val destino: String? = null,
    val tipoMercancia: String? = null,
    val imo: String? = null,
    val mmsi: String? = null,
    val tipoBuque: String? = null,
    val anioConstruccion: String? = null,
    val urlFoto: String? = null,
    /** true cuando ya se ha intentado consultar VesselFinder (con exito o sin el). */
    val vesselFinderConsultado: Boolean = false,
    /**
     * true cuando el buque de VesselFinder se ha confirmado comparando su
     * eslora con la que publica el puerto. false = coincide el nombre pero no
     * se ha podido verificar el tamano, asi que puede ser un homonimo.
     */
    val coincidenciaVerificada: Boolean = false,
    /** Diferencia de eslora entre las dos fuentes, en metros. */
    val diferenciaEsloraM: Double? = null
) {
    /** Ficha de VesselFinder por IMO, mas precisa que la busqueda por nombre. */
    val urlFichaImo: String? get() = imo?.let { "https://www.vesselfinder.com/vessels/details/$it" }

    fun combinar(otro: DetalleBuque) = DetalleBuque(
        esloraM = esloraM ?: otro.esloraM,
        mangaM = mangaM ?: otro.mangaM,
        tonelajeBruto = tonelajeBruto ?: otro.tonelajeBruto,
        destino = destino ?: otro.destino,
        tipoMercancia = tipoMercancia ?: otro.tipoMercancia,
        imo = imo ?: otro.imo,
        mmsi = mmsi ?: otro.mmsi,
        tipoBuque = tipoBuque ?: otro.tipoBuque,
        anioConstruccion = anioConstruccion ?: otro.anioConstruccion,
        urlFoto = urlFoto ?: otro.urlFoto,
        vesselFinderConsultado = vesselFinderConsultado || otro.vesselFinderConsultado,
        coincidenciaVerificada = coincidenciaVerificada || otro.coincidenciaVerificada,
        diferenciaEsloraM = diferenciaEsloraM ?: otro.diferenciaEsloraM
    )
}

enum class Lista(val titulo: String, val url: String) {
    ENTRADAS("Entradas", "https://www.puertosantander.es/es/entradas-hoy"),
    SALIDAS("Salidas", "https://www.puertosantander.es/es/salidas-hoy"),
    EN_PUERTO("En puerto", "https://www.puertosantander.es/es/buques-en-el-puerto")
}

data class ResultadoLista(
    val buques: List<Buque> = emptyList(),
    /** Texto de la cabecera H1, p. ej. "Entradas hoy - 19/09/2026 22:15". */
    val encabezado: String? = null
)

enum class TipoMovimiento(val etiqueta: String) { ENTRADA("Entrada"), SALIDA("Salida") }

/**
 * La tabla del puerto no lista escalas sino ATRAQUES: un buque que fondea y
 * luego atraca, o que cambia de muelle, aparece varias veces el mismo dia con
 * el mismo numero de escala. Distinguir la entrada o salida real del puerto de
 * una maniobra interna evita que parezcan datos duplicados o erroneos.
 */
enum class ClaseMovimiento(val etiqueta: String, val descripcion: String) {
    ENTRADA_PUERTO("ENTRA", "Entrada al puerto"),
    ATRAQUE("ATRACA", "Cambio de atraque"),
    DESATRAQUE("DESATRACA", "Cambio de atraque"),
    SALIDA_PUERTO("SALE", "Salida del puerto"),
    FONDEA("FONDEA", "Llega al fondeadero"),
    LEVA("LEVA", "Abandona el fondeadero");

    val esManiobraInterna: Boolean get() = this == ATRAQUE || this == DESATRAQUE

    /** Movimientos en el fondeadero, fuera de la bahia. */
    val esFondeo: Boolean get() = this == FONDEA || this == LEVA
}

/**
 * Un movimiento del dia: la entrada o la salida de un buque, con su hora.
 * Es la unidad de la pantalla principal, que mezcla ambas cronologicamente.
 */
data class Movimiento(
    val buque: Buque,
    val tipo: TipoMovimiento,
    val momento: LocalDateTime?,
    val clase: ClaseMovimiento = ClaseMovimiento.ENTRADA_PUERTO,
    /** Numero de atraques que tiene hoy esta misma escala. */
    val atraquesDeLaEscala: Int = 1
) {
    val horaTexto: String
        get() = if (tipo == TipoMovimiento.ENTRADA) buque.atraqueInicioTexto
        else buque.atraqueFinTexto

    companion object {
        /**
         * Clasifica los movimientos de una jornada: dentro de cada escala
         * (mismo numero de registro), el primer atraque es la entrada real al
         * puerto y el ultimo desatraque la salida real; los intermedios son
         * cambios de muelle.
         */
        fun clasificar(movimientos: List<Movimiento>): List<Movimiento> {
            val porEscala = movimientos.groupBy { it.buque.escala }
            val clasificados = mutableMapOf<Movimiento, Movimiento>()

            for ((_, grupo) in porEscala) {
                // Los fondeos van por su cuenta: ni son entrada al puerto ni
                // salida de el, sino espera en el exterior.
                grupo.filter { it.buque.esFondeo }.forEach { m ->
                    clasificados[m] = m.copy(
                        clase = if (m.tipo == TipoMovimiento.ENTRADA) ClaseMovimiento.FONDEA
                        else ClaseMovimiento.LEVA,
                        atraquesDeLaEscala = grupo.count { !it.buque.esFondeo }
                    )
                }

                val enMuelle = grupo.filter { !it.buque.esFondeo }
                val entradas = enMuelle.filter { it.tipo == TipoMovimiento.ENTRADA }
                    .sortedWith(compareBy(nullsLast<LocalDateTime>()) { it.momento })
                val salidas = enMuelle.filter { it.tipo == TipoMovimiento.SALIDA }
                    .sortedWith(compareBy(nullsLast<LocalDateTime>()) { it.momento })

                entradas.forEachIndexed { i, m ->
                    clasificados[m] = m.copy(
                        clase = if (i == 0) ClaseMovimiento.ENTRADA_PUERTO
                        else ClaseMovimiento.ATRAQUE,
                        atraquesDeLaEscala = enMuelle.size
                    )
                }
                salidas.forEachIndexed { i, m ->
                    clasificados[m] = m.copy(
                        clase = if (i == salidas.lastIndex) ClaseMovimiento.SALIDA_PUERTO
                        else ClaseMovimiento.DESATRAQUE,
                        atraquesDeLaEscala = enMuelle.size
                    )
                }
            }
            return movimientos.map { clasificados[it] ?: it }
        }
    }
}
