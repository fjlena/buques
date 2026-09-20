package es.puertosantander.buques.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class EstadoLista(
    val cargando: Boolean = false,
    val buques: List<Buque> = emptyList(),
    val encabezado: String? = null,
    val error: String? = null
)

data class EstadoApp(
    val listas: Map<Lista, EstadoLista> = Lista.entries.associateWith { EstadoLista() },
    /** Datos tecnicos por buque (clave = nombre en mayusculas). */
    val detalles: Map<String, DetalleBuque> = emptyMap(),
    val ultimaActualizacion: String? = null
) {
    val cargando: Boolean get() = listas.values.any { it.cargando }

    fun de(lista: Lista): EstadoLista = listas[lista] ?: EstadoLista()

    fun detalle(buque: Buque): DetalleBuque? = detalles[buque.clave]

    /** Entradas y salidas del dia mezcladas y ordenadas cronologicamente. */
    val movimientos: List<Movimiento>
        get() {
            val entradas = de(Lista.ENTRADAS).buques.map {
                Movimiento(it, TipoMovimiento.ENTRADA, it.atraqueInicio)
            }
            val salidas = de(Lista.SALIDAS).buques.map {
                Movimiento(it, TipoMovimiento.SALIDA, it.atraqueFin)
            }
            val todos = (entradas + salidas)
                .sortedWith(compareBy(nullsLast<LocalDateTime>()) { it.momento })
            return Movimiento.clasificar(todos)
        }

    /** Movimientos en muelle: entradas, salidas y cambios de atraque. */
    val movimientosEnMuelle: List<Movimiento>
        get() = movimientos.filter { !it.clase.esFondeo }

    /** Movimientos en el fondeadero, fuera de la bahia. */
    val movimientosEnFondeo: List<Movimiento>
        get() = movimientos.filter { it.clase.esFondeo }

    /**
     * Primer movimiento de muelle cuya hora aun no ha pasado: es el que la
     * pantalla principal destaca. Los fondeos no compiten por ese puesto,
     * porque ocurren fuera de la bahia y no son lo que se ve desde el puerto.
     */
    fun proximo(ahora: LocalDateTime = LocalDateTime.now()): Movimiento? =
        movimientosEnMuelle.firstOrNull { m -> m.momento?.isAfter(ahora) == true }

    fun proximoFondeo(ahora: LocalDateTime = LocalDateTime.now()): Movimiento? =
        movimientosEnFondeo.firstOrNull { m -> m.momento?.isAfter(ahora) == true }

    /**
     * Atraque al que va un buque fondeado: se busca en las tres listas otra
     * fila de la misma escala con muelle real. Se prefiere la posterior al
     * fondeo; si las horas de la web no son coherentes (a veces el atraque
     * figura antes que el fondeo), se toma la primera que haya.
     */
    fun atraqueDeEscala(buque: Buque): Buque? {
        if (!buque.esFondeo) return null
        val enMuelle = Lista.entries
            .flatMap { de(it).buques }
            .filter { !it.esFondeo && it.escala == buque.escala && it.atraqueInicio != null }
            .distinctBy { it.muelle + it.atraqueInicioTexto }
        if (enMuelle.isEmpty()) return null

        val fondeoInicio = buque.atraqueInicio
        val posteriores = if (fondeoInicio == null) enMuelle
        else enMuelle.filter { !it.atraqueInicio!!.isBefore(fondeoInicio) }

        return (posteriores.ifEmpty { enMuelle }).minByOrNull { it.atraqueInicio!! }
    }
}

class BuquesViewModel : ViewModel() {

    private val _estado = MutableStateFlow(EstadoApp())
    val estado: StateFlow<EstadoApp> = _estado.asStateFlow()

    private val reloj = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    /** Limita a 3 las peticiones simultaneas de fichas al servidor del puerto. */
    private val semaforo = Semaphore(3)

    init {
        actualizar()
    }

    /** Recarga las tres listas en paralelo y despues completa las esloras. */
    fun actualizar() {
        if (_estado.value.cargando) return

        _estado.update { app ->
            app.copy(listas = app.listas.mapValues { (_, e) -> e.copy(cargando = true, error = null) })
        }

        viewModelScope.launch {
            Lista.entries.map { lista ->
                async {
                    val resultado = runCatching { PuertoRepository.cargar(lista) }
                    _estado.update { app ->
                        val nuevo = resultado.fold(
                            onSuccess = { r ->
                                EstadoLista(
                                    cargando = false,
                                    buques = ordenar(lista, r.buques),
                                    encabezado = r.encabezado
                                )
                            },
                            onFailure = { t ->
                                app.de(lista).copy(cargando = false, error = mensajeError(t))
                            }
                        )
                        app.copy(listas = app.listas + (lista to nuevo))
                    }
                }
            }.awaitAll()

            _estado.update { it.copy(ultimaActualizacion = LocalDateTime.now().format(reloj)) }
            cargarEsloras()
        }
    }

    /**
     * Completa eslora, tonelaje y destino desde la ficha de cada buque en la web
     * del puerto. Se pide una sola vez por buque (los datos tecnicos no cambian)
     * y con un maximo de tres peticiones simultaneas para no saturar el servidor.
     */
    private fun cargarEsloras() {
        val app = _estado.value
        val pendientes = Lista.entries
            .flatMap { app.de(it).buques }
            .filter { it.urlPuerto != null && app.detalles[it.clave]?.esloraM == null }
            .distinctBy { it.clave }

        pendientes.forEach { buque ->
            viewModelScope.launch {
                val detalle = semaforo.withPermit {
                    runCatching { PuertoRepository.cargarDetalle(buque.urlPuerto!!) }.getOrNull()
                } ?: return@launch
                fusionar(buque.clave, detalle)
            }
        }
    }

    /**
     * Consulta VesselFinder para un buque concreto (foto, IMO, MMSI, tipo).
     * Se llama al abrir su ficha, no al cargar las listas.
     *
     * Antes de buscar se asegura de tener la eslora del puerto: es la que
     * permite descartar homonimos (el ferry SALAMANCA de 214 m frente a los
     * veleros del mismo nombre). Si no hay eslora de referencia se busca
     * igualmente, pero el resultado queda marcado como no verificado.
     */
    fun cargarVesselFinder(buque: Buque) {
        val actual = _estado.value.detalles[buque.clave]
        if (actual?.vesselFinderConsultado == true) return

        viewModelScope.launch {
            var eslora = actual?.esloraM
            if (eslora == null && buque.urlPuerto != null) {
                val delPuerto = semaforo.withPermit {
                    runCatching { PuertoRepository.cargarDetalle(buque.urlPuerto) }.getOrNull()
                }
                if (delPuerto != null) {
                    fusionar(buque.clave, delPuerto)
                    eslora = delPuerto.esloraM
                }
            }
            val detalle = VesselFinderRepository.cargar(buque.nombre, eslora)
            fusionar(buque.clave, detalle)
        }
    }

    private fun fusionar(clave: String, detalle: DetalleBuque) {
        _estado.update { app ->
            val previo = app.detalles[clave]
            val combinado = if (previo == null) detalle else detalle.combinar(previo)
            app.copy(detalles = app.detalles + (clave to combinado))
        }
    }

    /**
     * Entradas y salidas se ordenan por la hora del movimiento, ascendente.
     * "En puerto" conserva el orden de la web, que viene agrupado por atraque:
     * es la forma en que se lee el ocupado de los muelles.
     */
    private fun ordenar(lista: Lista, buques: List<Buque>): List<Buque> = when (lista) {
        Lista.EN_PUERTO -> buques
        Lista.SALIDAS ->
            buques.sortedWith(compareBy(nullsLast<LocalDateTime>()) { it.atraqueFin })
        Lista.ENTRADAS ->
            buques.sortedWith(compareBy(nullsLast<LocalDateTime>()) { it.atraqueInicio })
    }

    private fun mensajeError(t: Throwable): String = when (t) {
        is java.net.UnknownHostException -> "Sin conexion a internet"
        is java.net.SocketTimeoutException -> "La web del puerto no responde"
        else -> t.message ?: "Error al descargar los datos"
    }
}
