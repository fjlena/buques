package es.puertosantander.buques.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Obtiene de VesselFinder lo que la web del puerto no publica: la fotografía
 * del buque, su IMO/MMSI, el tipo y el año de construcción.
 *
 * VesselFinder no tiene API pública gratuita y la ficha del puerto no incluye
 * el IMO, así que hay que dar dos pasos: buscar por nombre y, del primer
 * resultado, abrir su ficha. Si algo falla (nombre ambiguo, sin foto, respuesta
 * denegada) se devuelve un detalle vacío marcado como ya consultado: la
 * aplicación sigue funcionando con los datos del puerto.
 *
 * Se consulta solo al abrir la ficha de un buque, nunca para toda la lista:
 * serían decenas de peticiones a un servidor ajeno en cada actualización.
 */
object VesselFinderRepository {

    private const val BASE = "https://www.vesselfinder.com"

    suspend fun cargar(nombre: String): DetalleBuque = withContext(Dispatchers.IO) {
        try {
            val ficha = fichaDe(nombre) ?: return@withContext DetalleBuque(vesselFinderConsultado = true)
            parsearFicha(ficha)
        } catch (e: Exception) {
            DetalleBuque(vesselFinderConsultado = true)
        }
    }

    /** Busca el buque por nombre y devuelve el documento de su ficha. */
    private fun fichaDe(nombre: String): Document? {
        val consulta = java.net.URLEncoder.encode(nombre, "UTF-8")
        val busqueda = descargar("$BASE/vessels?name=$consulta")

        // Si la búsqueda ya ha redirigido a una ficha, el enlace apunta a sí misma.
        val enlace = busqueda.selectFirst("a[href*=/vessels/details/]")?.absUrl("href")
            ?: return null

        return if (busqueda.location().contains("/vessels/details/")) busqueda
        else descargar(enlace)
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
