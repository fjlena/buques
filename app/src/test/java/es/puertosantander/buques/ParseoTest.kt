package es.puertosantander.buques

import es.puertosantander.buques.data.PuertoRepository
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
}
