package es.puertosantander.buques.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.Normalizer

/**
 * Descarga y parsea las paginas publicas de puertosantander.es.
 *
 * El parseo es deliberadamente tolerante: no depende de las clases CSS del tema
 * de Drupal (que cambian con cada rediseno), sino de la forma de los datos
 * (el enlace /es/buque/..., las fechas dd/MM/yyyy, el codigo de bandera de dos
 * letras, el registro nnnn/aaaa). Asi una reforma de la web tiene menos
 * probabilidades de romper la aplicacion.
 */
object PuertoRepository {

    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"

    private val REGEX_FECHA = Regex("""\d{2}/\d{2}/\d{4}\s*-\s*\d{2}:\d{2}""")
    private val REGEX_REGISTRO = Regex("""^\d{1,6}/\d{4}$""")
    private val REGEX_BANDERA = Regex("""^[A-Z]{2}$""")
    private val REGEX_NOMBRE = Regex("""^(.*?)\s*\(([^)]*)\)\s*$""")

    suspend fun cargar(lista: Lista): ResultadoLista = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(lista.url)
            .userAgent(USER_AGENT)
            .header("Accept-Language", "es-ES,es;q=0.9")
            .timeout(25_000)
            .get()
        parsear(doc)
    }

    /**
     * Ficha del buque en la web del puerto: de aqui salen la eslora (campo
     * "Longitud"), el tonelaje bruto, el destino y el tipo de mercancia.
     */
    suspend fun cargarDetalle(urlFicha: String): DetalleBuque = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(urlFicha)
            .userAgent(USER_AGENT)
            .header("Accept-Language", "es-ES,es;q=0.9")
            .timeout(20_000)
            .get()
        parsearDetalle(doc)
    }

    internal fun parsear(doc: Document): ResultadoLista {
        val encabezado = doc.selectFirst("h1")?.text()?.trim()
        val tabla = elegirTabla(doc) ?: return ResultadoLista(emptyList(), encabezado)

        val buques = mutableListOf<Buque>()
        var muelleActual = ""

        for (fila in tabla.select("tr")) {
            val celdas = fila.select("td")
            if (celdas.isEmpty()) continue // fila de cabecera (th)

            val enlace = fila.selectFirst("a[href*=/buque/]")

            if (enlace == null) {
                // Fila de agrupacion: el nombre del muelle o atraque.
                val texto = fila.text().trim()
                if (texto.isNotEmpty() && texto.length < 80) muelleActual = texto
                continue
            }

            val textos = celdas.map { limpiar(it.text()) }
            val fechas = REGEX_FECHA.findAll(fila.text()).map { it.value.normalizarFecha() }.toList()

            val nombreCompleto = limpiar(enlace.text())
            val m = REGEX_NOMBRE.find(nombreCompleto)
            val nombre = (m?.groupValues?.get(1) ?: nombreCompleto).trim()
            val procedencia = m?.groupValues?.get(2)?.trim().orEmpty()

            val registro = textos.firstOrNull { REGEX_REGISTRO.matches(it) }.orEmpty()
            val bandera = textos.firstOrNull { REGEX_BANDERA.matches(it) }
                ?: banderaDeImagen(fila).orEmpty()
            val consignatario = textos
                .lastOrNull { it.isNotEmpty() && !REGEX_FECHA.containsMatchIn(it) && it != nombreCompleto }
                .orEmpty()

            buques += Buque(
                nombre = nombre,
                procedencia = procedencia,
                nombreCompleto = nombreCompleto,
                registro = registro,
                bandera = bandera,
                muelle = muelleActual,
                atraqueInicioTexto = fechas.getOrElse(0) { "" },
                atraqueFinTexto = fechas.getOrElse(1) { "" },
                consignatario = if (consignatario == bandera || consignatario == registro) "" else consignatario,
                urlPuerto = enlace.absUrl("href").ifBlank { null }
            )
        }
        return ResultadoLista(buques, encabezado)
    }

    internal fun parsearDetalle(doc: Document): DetalleBuque {
        val datos = paresEtiquetaValor(doc)
        return DetalleBuque(
            esloraM = numero(datos["longitud"]),
            tonelajeBruto = numero(datos["tonelaje bruto"]),
            destino = texto(datos["destino"]),
            tipoMercancia = texto(datos["tipo de mercancia"])
        )
    }

    /** Se queda con la tabla que tenga mas enlaces a fichas de buque. */
    private fun elegirTabla(doc: Document): Element? =
        doc.select("table").maxByOrNull { it.select("a[href*=/buque/]").size }
            ?.takeIf { it.select("a[href*=/buque/]").isNotEmpty() }

    private fun banderaDeImagen(fila: Element): String? {
        val src = fila.selectFirst("img[src*=country-flags]")?.attr("src") ?: return null
        return src.substringAfterLast('/').substringBefore('.').uppercase().takeIf { it.length == 2 }
    }

    private fun limpiar(s: String): String =
        s.replace('\u00A0', ' ').trim().replace(Regex("""\s{2,}"""), " ")

    private fun String.normalizarFecha(): String {
        val partes = split("-")
        return if (partes.size == 2) "${partes[0].trim()} - ${partes[1].trim()}" else trim()
    }

    // --- Utilidades compartidas con el scraper de VesselFinder ---------------

    /**
     * Recorre todas las filas de dos celdas del documento y devuelve un mapa
     * etiqueta -> valor, con la etiqueta en minusculas y sin acentos. Sirve
     * tanto para las fichas del puerto como para las de VesselFinder, que
     * usan el mismo patron de tabla de dos columnas.
     */
    fun paresEtiquetaValor(doc: Document): Map<String, String> {
        val mapa = mutableMapOf<String, String>()
        for (fila in doc.select("tr")) {
            val celdas = fila.select("td, th")
            if (celdas.size != 2) continue
            val clave = sinAcentos(celdas[0].text()).lowercase().trim().trimEnd(':')
            val valor = celdas[1].text().trim()
            if (clave.isNotEmpty() && clave !in mapa) mapa[clave] = valor
        }
        return mapa
    }

    fun sinAcentos(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "")

    /** "140.6400" -> 140.64; "7.852" o "---" -> null cuando no hay dato. */
    fun numero(valor: String?): Double? {
        val v = texto(valor) ?: return null
        val limpio = v.replace(Regex("""[^0-9.,\-]"""), "").replace(',', '.')
        val n = limpio.toDoubleOrNull() ?: return null
        return if (n <= 0.0) null else n
    }

    /** Convierte los marcadores de "sin dato" de la web en null. */
    fun texto(valor: String?): String? {
        val v = valor?.trim().orEmpty()
        if (v.isEmpty()) return null
        if (v.all { it == '-' } || v == "N/A" || v == "-") return null
        return v
    }
}
