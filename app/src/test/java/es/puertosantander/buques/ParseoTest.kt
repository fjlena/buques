package es.puertosantander.buques

import es.puertosantander.buques.data.PuertoRepository
import es.puertosantander.buques.data.VesselFinderRepository
import es.puertosantander.buques.data.Buque
import es.puertosantander.buques.data.ClaseMovimiento
import es.puertosantander.buques.data.EstadoApp
import es.puertosantander.buques.data.EstadoLista
import es.puertosantander.buques.data.Lista
import es.puertosantander.buques.data.Movimiento
import es.puertosantander.buques.data.TipoMovimiento
import org.junit.Assert.assertFalse
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

    // --- Identificacion del buque correcto entre homonimos -------------------

    @Test
    fun descartaHomonimosDeEsloraIncompatible() {
        // El ferry SALAMANCA mide 214,5 m; un velero del mismo nombre, unos 20.
        assertEquals(false, VesselFinderRepository.coincideEslora(214.5, 20.0))
        assertEquals(true, VesselFinderRepository.coincideEslora(214.5, 215.0))
        // Margen por redondeos y errores de la fuente.
        assertEquals(true, VesselFinderRepository.coincideEslora(140.64, 141.0))
        assertEquals(true, VesselFinderRepository.coincideEslora(140.64, 147.0))
        assertEquals(false, VesselFinderRepository.coincideEslora(140.64, 175.0))
        // Sin eslora de referencia no se puede juzgar.
        assertEquals(null, VesselFinderRepository.coincideEslora(null, 215.0))
    }

    @Test
    fun comparaNombresIgnorandoSignosYAcentos() {
        assertEquals(
            VesselFinderRepository.normalizar("LADY ANNE-LYNN"),
            VesselFinderRepository.normalizar("Lady Anne Lynn")
        )
        assertFalse(
            VesselFinderRepository.normalizar("SALAMANCA") ==
                VesselFinderRepository.normalizar("SALAMANCA II")
        )
    }

    // --- Atraques repetidos de una misma escala ------------------------------

    private fun buque(registro: String, muelle: String, entra: String, sale: String) = Buque(
        nombre = "AUTOSKY",
        procedencia = "FR/LEH",
        nombreCompleto = "AUTOSKY (FR/LEH)",
        registro = registro,
        bandera = "PT",
        muelle = muelle,
        atraqueInicioTexto = entra,
        atraqueFinTexto = sale,
        consignatario = "NOATUM MARITIME SPAIN, S.A.",
        urlPuerto = null
    )

    @Test
    fun distingueCambiosDeAtraqueDeEntradasYSalidasReales() {
        // Caso real: la misma escala 1141/2026 figura dos veces el mismo dia.
        val primero = buque("1141/2026", "RAOS 8 OESTE", "20/09/2026 - 13:30", "20/09/2026 - 22:00")
        val segundo = buque("1141/2026", "RAOS 8 ESTE", "20/09/2026 - 22:00", "22/09/2026 - 06:00")

        val movimientos = Movimiento.clasificar(
            listOf(
                Movimiento(primero, TipoMovimiento.ENTRADA, primero.atraqueInicio),
                Movimiento(segundo, TipoMovimiento.ENTRADA, segundo.atraqueInicio),
                Movimiento(primero, TipoMovimiento.SALIDA, primero.atraqueFin),
                Movimiento(segundo, TipoMovimiento.SALIDA, segundo.atraqueFin)
            )
        )

        val entradas = movimientos.filter { it.tipo == TipoMovimiento.ENTRADA }
        val salidas = movimientos.filter { it.tipo == TipoMovimiento.SALIDA }

        // Solo el primer atraque es entrada al puerto; el segundo es maniobra.
        assertEquals(ClaseMovimiento.ENTRADA_PUERTO, entradas.first().clase)
        assertEquals(ClaseMovimiento.ATRAQUE, entradas.last().clase)
        // Solo el ultimo desatraque es salida del puerto.
        assertEquals(ClaseMovimiento.DESATRAQUE, salidas.first().clase)
        assertEquals(ClaseMovimiento.SALIDA_PUERTO, salidas.last().clase)
        assertTrue(entradas.last().clase.esManiobraInterna)
        assertEquals(4, entradas.first().atraquesDeLaEscala)
    }

    @Test
    fun unaEscalaConUnSoloAtraqueEsEntradaYSalidaReal() {
        val unico = buque("1200/2026", "RAOS 3", "20/09/2026 - 08:00", "20/09/2026 - 20:00")
        val movimientos = Movimiento.clasificar(
            listOf(
                Movimiento(unico, TipoMovimiento.ENTRADA, unico.atraqueInicio),
                Movimiento(unico, TipoMovimiento.SALIDA, unico.atraqueFin)
            )
        )
        assertEquals(ClaseMovimiento.ENTRADA_PUERTO, movimientos[0].clase)
        assertEquals(ClaseMovimiento.SALIDA_PUERTO, movimientos[1].clase)
    }

    // --- Fondeos separados de los atraques -----------------------------------

    private fun fondeo(registro: String, entra: String, sale: String) = Buque(
        nombre = "KARIN",
        procedencia = "BE/ANR",
        nombreCompleto = "KARIN (BE/ANR)",
        registro = registro,
        bandera = "AG",
        muelle = "FONDEO",
        atraqueInicioTexto = entra,
        atraqueFinTexto = sale,
        consignatario = "COBASA, S.A.",
        urlPuerto = null
    )

    @Test
    fun elFondeoNoEsEntradaNiSalidaDelPuerto() {
        val espera = fondeo("1181/2026", "19/09/2026 - 22:15", "21/09/2026 - 16:00")
        val atraque = buque("1181/2026", "RAOS 5", "21/09/2026 - 17:00", "22/09/2026 - 20:00")

        val movimientos = Movimiento.clasificar(
            listOf(
                Movimiento(espera, TipoMovimiento.ENTRADA, espera.atraqueInicio),
                Movimiento(espera, TipoMovimiento.SALIDA, espera.atraqueFin),
                Movimiento(atraque, TipoMovimiento.ENTRADA, atraque.atraqueInicio),
                Movimiento(atraque, TipoMovimiento.SALIDA, atraque.atraqueFin)
            )
        )

        assertEquals(ClaseMovimiento.FONDEA, movimientos[0].clase)
        assertEquals(ClaseMovimiento.LEVA, movimientos[1].clase)
        assertTrue(movimientos[0].clase.esFondeo)

        // El atraque posterior sigue siendo la entrada real al puerto, no una
        // maniobra interna: el fondeo no le ha quitado ese papel.
        assertEquals(ClaseMovimiento.ENTRADA_PUERTO, movimientos[2].clase)
        assertEquals(ClaseMovimiento.SALIDA_PUERTO, movimientos[3].clase)
        assertFalse(movimientos[2].clase.esManiobraInterna)

        assertTrue(espera.esFondeo)
        assertFalse(atraque.esFondeo)
        assertEquals(espera.escala, atraque.escala)
    }

    @Test
    fun encuentraElAtraqueAlQueVaUnBuqueFondeado() {
        val espera = fondeo("1181/2026", "19/09/2026 - 22:15", "21/09/2026 - 16:00")
        val destino = buque("1181/2026", "RAOS 5", "21/09/2026 - 17:00", "22/09/2026 - 20:00")

        val estado = EstadoApp(
            listas = mapOf(
                Lista.ENTRADAS to EstadoLista(buques = listOf(espera, destino)),
                Lista.SALIDAS to EstadoLista(),
                Lista.EN_PUERTO to EstadoLista(buques = listOf(espera))
            )
        )

        assertEquals("RAOS 5", estado.atraqueDeEscala(espera)?.muelle)
        // Un buque atracado no tiene "atraque previsto": ya esta en el suyo.
        assertEquals(null, estado.atraqueDeEscala(destino))

        // El fondeo no compite por ser el proximo movimiento de muelle.
        assertTrue(estado.movimientosEnFondeo.isNotEmpty())
        assertTrue(estado.movimientosEnMuelle.none { it.buque.esFondeo })
    }

    @Test
    fun elAtraqueAnteriorAlFondeoSirveDeRespaldo() {
        // Caso real: el LUCIA B tiene el fondeo a las 08:00 y el atraque en
        // RAOS 3 a las 03:50, antes. La web no siempre es coherente.
        val espera = fondeo("1106/2026", "19/09/2026 - 08:00", "19/09/2026 - 09:00")
        val antes = buque("1106/2026", "RAOS 3", "19/09/2026 - 03:50", "19/09/2026 - 21:35")

        val estado = EstadoApp(
            listas = mapOf(Lista.ENTRADAS to EstadoLista(buques = listOf(espera, antes)))
        )
        assertEquals("RAOS 3", estado.atraqueDeEscala(espera)?.muelle)
    }
}
