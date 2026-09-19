package es.puertosantander.buques

import es.puertosantander.buques.data.PuertoRepository
import es.puertosantander.buques.data.VesselFinderRepository
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprueba el parseo con una tabla de ejemplo con la misma estructura que
 * publica puertosantander.es. Si el puerto rediseña la web y la app deja de
 * mostrar datos, actualizar este HTML de ejemplo es la vía rápida para ver
 * qué ha cambiado: `./gradlew test`.
 */
class ParseoTest {

    private val html = """
        <html><body>
        <h1>Entradas hoy - 19/09/2026 22:15</h1>
        <table>
          <thead><tr><th>Registro</th><th>Buque y procedencia</th><th>Bandera</th>
          <th>Comienzo</th><th>Final</th><th>Consignatario</th></tr></thead>
          <tbody>
            <tr><td colspan="6">FONDEO</td></tr>
            <tr>
              <td>1106/2026</td>
              <td><a href="/es/buque/lucia-b-gbliv">LUCIA B (GB/LIV)</a></td>
              <td>PT</td>
              <td>19/09/2026 - 08:00</td>
              <td>19/09/2026 - 09:00</td>
              <td>MILLER Y CIA. S.A.</td>
            </tr>
            <tr><td colspan="6">RAOS 3</td></tr>
            <tr>
              <td>1101/2026</td>
              <td><a href="/es/buque/filyoz-frlav">FILYOZ (FR/LAV)</a></td>
              <td>MT</td>
              <td>20/09/2026 - 11:00</td>
              <td>20/09/2026 - 17:00</td>
              <td>CANTABRIASIL, S.A.</td>
            </tr>
          </tbody>
        </table>
        </body></html>
    """.trimIndent()

    @Test
    fun extraeLosBuquesConSusDatos() {
        val doc = Jsoup.parse(html, "https://www.puertosantander.es/")
        val r = PuertoRepository.parsear(doc)

        assertEquals(2, r.buques.size)
        assertTrue(r.encabezado!!.startsWith("Entradas hoy"))

        val primero = r.buques[0]
        assertEquals("LUCIA B", primero.nombre)
        assertEquals("GB/LIV", primero.procedencia)
        assertEquals("1106/2026", primero.registro)
        assertEquals("PT", primero.bandera)
        assertEquals("FONDEO", primero.muelle)
        assertEquals("19/09/2026 - 08:00", primero.atraqueInicioTexto)
        assertEquals("19/09/2026 - 09:00", primero.atraqueFinTexto)
        assertEquals("MILLER Y CIA. S.A.", primero.consignatario)
        assertEquals(9, primero.atraqueFin!!.hour)

        assertEquals("RAOS 3", r.buques[1].muelle)
        assertTrue(r.buques[1].urlVesselFinder.contains("name=FILYOZ"))
    }

    private val fichaPuerto = """
        <html><body>
        <h1>LUCIA B (GB/LIV)</h1>
        <table>
          <tr><td>Nombre</td><td><strong>LUCIA B (GB/LIV)</strong></td></tr>
          <tr><td>Codigo Pais</td><td><strong>PT</strong></td></tr>
          <tr><td>Destino</td><td><strong>---</strong></td></tr>
          <tr><td>Tipo de mercancia</td><td><strong>---</strong></td></tr>
          <tr><td>Tonelaje bruto</td><td><strong>7852.000</strong></td></tr>
          <tr><td>Longitud</td><td><strong>140.6400</strong></td></tr>
          <tr><td>Consignatario</td><td><strong>MILLER Y CIA. S.A.</strong></td></tr>
        </table>
        </body></html>
    """.trimIndent()

    @Test
    fun extraeLaEsloraDeLaFichaDelPuerto() {
        val d = PuertoRepository.parsearDetalle(Jsoup.parse(fichaPuerto))

        assertEquals(140.64, d.esloraM!!, 0.001)
        assertEquals(7852.0, d.tonelajeBruto!!, 0.001)
        // Los "---" de la web no deben convertirse en texto.
        assertEquals(null, d.destino)
        assertEquals(null, d.tipoMercancia)
    }

    private val fichaVesselFinder = """
        <html><body>
        <h1>LUCIA B</h1>
        <a href="/ship-photos/1122612">
          <img src="https://static.vesselfinder.net/ship-photo/9404077-255806171-abc/1?v1" alt="LUCIA B photo">
        </a>
        <table>
          <tr><td>IMO / MMSI</td><td>9404077 / 255806171</td></tr>
          <tr><td>Length / Beam</td><td>141 / 22 m</td></tr>
        </table>
        <table>
          <tr><td>IMO number</td><td>9404077</td></tr>
          <tr><td>Ship Type</td><td>Container Ship</td></tr>
          <tr><td>Year of Build</td><td>2007</td></tr>
          <tr><td>Length Overall (m)</td><td>140.64</td></tr>
          <tr><td>Beam (m)</td><td>21.80</td></tr>
          <tr><td>Gross Tonnage</td><td>7852</td></tr>
        </table>
        </body></html>
    """.trimIndent()

    @Test
    fun extraeFotoYDatosDeVesselFinder() {
        val doc = Jsoup.parse(fichaVesselFinder, "https://www.vesselfinder.com/vessels/details/9404077")
        val d = VesselFinderRepository.parsearFicha(doc)

        assertEquals("9404077", d.imo)
        assertEquals("255806171", d.mmsi)
        assertEquals("Container Ship", d.tipoBuque)
        assertEquals("2007", d.anioConstruccion)
        assertEquals(140.64, d.esloraM!!, 0.001)
        assertEquals(21.80, d.mangaM!!, 0.001)
        assertTrue(d.urlFoto!!.contains("/ship-photo/"))
        assertEquals("https://www.vesselfinder.com/vessels/details/9404077", d.urlFichaImo)
    }
}
