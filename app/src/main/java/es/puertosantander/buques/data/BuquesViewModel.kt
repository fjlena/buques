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
    val ultimaActualizacion: String? = null
) {
    val cargando: Boolean get() = listas.values.any { it.cargando }
    fun de(lista: Lista): EstadoLista = listas[lista] ?: EstadoLista()
}

class BuquesViewModel : ViewModel() {

    private val _estado = MutableStateFlow(EstadoApp())
    val estado: StateFlow<EstadoApp> = _estado.asStateFlow()

    private val reloj = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    init {
        // Primera carga al abrir la aplicación.
        actualizar()
    }

    /** Recarga las tres listas en paralelo. */
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
                                app.de(lista).copy(
                                    cargando = false,
                                    error = mensajeError(t)
                                )
                            }
                        )
                        app.copy(listas = app.listas + (lista to nuevo))
                    }
                }
            }.awaitAll()

            _estado.update { it.copy(ultimaActualizacion = LocalDateTime.now().format(reloj)) }
        }
    }

    /**
     * En "En puerto" lo importante es la salida prevista, así que se ordena por
     * ella de forma ascendente: primero los que se van antes.
     * Entradas y salidas se ordenan por la hora del movimiento del día.
     */
    private fun ordenar(lista: Lista, buques: List<Buque>): List<Buque> = when (lista) {
        Lista.EN_PUERTO, Lista.SALIDAS ->
            buques.sortedWith(compareBy(nullsLast<LocalDateTime>()) { it.atraqueFin })
        Lista.ENTRADAS ->
            buques.sortedWith(compareBy(nullsLast<LocalDateTime>()) { it.atraqueInicio })
    }

    private fun mensajeError(t: Throwable): String = when (t) {
        is java.net.UnknownHostException -> "Sin conexión a internet"
        is java.net.SocketTimeoutException -> "La web del puerto no responde"
        else -> t.message ?: "Error al descargar los datos"
    }
}
