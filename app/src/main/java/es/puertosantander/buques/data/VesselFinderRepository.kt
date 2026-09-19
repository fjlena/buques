package es.puertosantander.buques.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.math.abs
import kotlin.math.max

/**
 * Obtiene de VesselFinder lo que la web del puerto no publica: la fotografia
 * del buque, su IMO/MMSI, el tipo y el ano de construccion.
 *
 * El problema de buscar por nombre es que hay homonimos de tamanos muy
 * distintos: "SALAMANCA" es a la vez un ferry de 214 m y varios veleros. Para
 * no confundirlos se comprueba la ESLORA de cada candidato contra la que
 * publica el puerto, con un margen generoso por si alguna de las dos fuentes
 * tiene el dato redondeado o mal. Solo si cuadra se acepta la ficha.
 */
object VesselFinderRepository {

    private const val BASE = "https://www.vesselfinder.com"

    /** Numero maximo de fichas candidatas que se abren por busqueda. */
    private const val MAX_CANDIDATOS = 5

    /**
     * Margen admitido entre la eslora del puerto y la de VesselFinder:
     * el mayor de 8 metros o el 8 % de la eslora. Cubre redondeos y errores
     * de tecleo sin llegar a confundir un mercante con un velero.
     */
    internal fun tolerancia(esloraM: Double): Double = max(8.0, esloraM * 0.08)

    internal fun coincideEslora(referencia: Double?, candidata: Double?): Boolean? {
        if (referencia == null || candidata == null) return null // no se puede juzgar
        return abs(referencia - candidata) <= tolerancia(referencia)
    }

    /** Nombres comparables: solo letras y digitos, en mayusculas. */
    internal fun normalizar(nombre: String): String =
        PuertoRepository.sinAcentos(nombre).uppercase().filter { it.isLetterOrDigit() }

    /**
     * @param esloraReferenciaM eslora segun la web del puerto, si se conoce.
     *        Sin ella no se puede verificar la identidad y el resultado se
     *        marca como no verificado.
     */
    suspend fun cargar(nombre: String, esloraReferenciaM: Double?): DetalleBuque =
        withContext(Dispatchers.IO) {
            try {
                buscar(nombre, esloraReferenciaM)
            } catch (e: Exception) {
                DetalleBuque(vesselFinderConsultado = true)
            }
        }

    private fun buscar(nombre: String, esloraReferenciaM: Double?): DetalleBuque {
        val consulta = java.net.URLEncoder.encode(nombre, "UTF-8")
        val busqueda = descargar("$BASE/vessels?name=$consulta")

        // La busqueda puede haber redirigido ya a una ficha concreta.
        val fichas: List<Document> =
            if (busqueda.location().contains("/vessels/details/")) listOf(busqueda)
            else busqueda.select("a[href*=/vessels/details/]")
                .map { it.absUrl("href") }
                .filter { it.isNotBlank() }
                .distinct()
                .take(MAX_CANDIDATOS)
                .mapNotNull { url -> runCatching { descargar(url) }.getOrNull() }

        if (fichas.isEmpty()) return DetalleBuque(vesselFinderConsultado = true)

        val buscado = normalizar(nombre)
        var mejorPorNombre: DetalleBuque? = null
        var mejorPorEslora: Pair<Double, DetalleBuque>? = null

        for (ficha in fichas) {
            val detalle = parsearFicha(ficha)
            if (normalizar(nombreDeFicha(ficha) ?: "") != buscado) continue

            when (coincideEslora(esloraReferenciaM, detalle.esloraM)) {
                true -> {
                    // Nombre y eslora cuadran: identificacion fiable, se acepta ya.
                    val diferencia = abs(esloraReferenciaM!! - detalle.esloraM!!)
                    return detalle.copy(
                        coincidenciaVerificada = true,
                        diferenciaEsloraM = diferencia
                    )
                }
                false -> {
                    // Mismo nombre pero tamano incompatible: se guarda por si
                    // ningun candidato cuadra, pero no se da por bueno.
                    val diferencia = abs(esloraReferenciaM!! - detalle.esloraM!!)
                    if (mejorPorEslora == null || diferencia < mejorPorEslora!!.first) {
                        mejorPorEslora = diferencia to detalle
                    }
                }
                null -> if (mejorPorNombre == null) mejorPorNombre = detalle
            }
        }

        // Sin verificacion posible: se devuelve el mejor candidato advertido
        // como no verificado, para que la ficha lo indique al usuario.
        mejorPorNombre?.let { return it.copy(coincidenciaVerificada = false) }
        mejorPorEslora?.let { (diferencia, detalle) ->
            return detalle.copy(coincidenciaVerificada = false, diferenciaEsloraM = diferencia)
        }
        return DetalleBuque(vesselFinderConsultado = true)
    }

    /** Nombre del buque segun la ficha: la tabla de particulares o el titulo. */
    internal fun nombreDeFicha(doc: Document): String? {
        val datos = PuertoRepository.paresEtiquetaValor(doc)
        return PuertoRepository.texto(datos["vessel name"])
            ?: doc.selectFirst("h1")?.text()?.trim()
    }

    internal fun parsearFicha(doc: Document): DetalleBuque {
        val datos = PuertoRepository.paresEtiquetaValor(doc)

        val imoMmsi = datos["imo / mmsi"]?.split("/")?.map { it.trim() }
        val largoAncho = datos["length / beam"]
            ?.replace("m", "")?.split("/")?.map { it.trim() }

        return DetalleBuque(
            esloraM = PuertoRepository.numero(datos["length overall (m)"])
                ?: PuertoRepository.numero(largoAncho?.getOrNull(0)),
            mangaM = PuertoRepository.numero(datos["beam (m)"])
                ?: PuertoRepository.numero(largoAncho?.getOrNull(1)),
            tonelajeBruto = PuertoRepository.numero(datos["gross tonnage"]),
            imo = PuertoRepository.texto(datos["imo number"] ?: imoMmsi?.getOrNull(0)),
            mmsi = PuertoRepository.texto(datos["mmsi"] ?: imoMmsi?.getOrNull(1)),
            tipoBuque = PuertoRepository.texto(datos["ship type"] ?: datos["ais type"]),
            anioConstruccion = PuertoRepository.texto(datos["year of build"]),
            urlFoto = foto(doc),
            vesselFinderConsultado = true
        )
    }

    /**
     * La foto del buque se sirve desde static.vesselfinder.net/ship-photo/...
     * Si no hay ninguna subida, la ficha no la incluye y se devuelve null.
     */
    private fun foto(doc: Document): String? {
        doc.selectFirst("img[src*=/ship-photo/]")?.absUrl("src")
            ?.takeIf { it.isNotBlank() }?.let { return it }
        return doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?.takeIf { it.contains("ship-photo") }
    }

    private fun descargar(url: String): Document =
        Jsoup.connect(url)
            .userAgent(PuertoRepository.USER_AGENT)
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .referrer("$BASE/")
            .timeout(20_000)
            .followRedirects(true)
            .get()
}
