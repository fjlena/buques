package es.puertosantander.buques

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.puertosantander.buques.data.Buque
import es.puertosantander.buques.data.BuquesViewModel
import es.puertosantander.buques.data.EstadoApp
import es.puertosantander.buques.data.EstadoLista
import es.puertosantander.buques.data.Lista
import es.puertosantander.buques.data.TipoMovimiento
import es.puertosantander.buques.ui.BuqueCard
import es.puertosantander.buques.ui.BuquesTheme
import es.puertosantander.buques.ui.CabeceraAtraque
import es.puertosantander.buques.ui.CabeceraFondeadero
import es.puertosantander.buques.ui.DialogoInformacion
import es.puertosantander.buques.ui.FichaVesselFinder
import es.puertosantander.buques.ui.MovimientoCard
import es.puertosantander.buques.ui.tiempoRelativo
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BuquesTheme {
                App()
            }
        }
    }
}

/** Pestanas de la aplicacion. La primera mezcla entradas y salidas. */
private enum class Pantalla(val titulo: String) {
    PRINCIPAL("Hoy"),
    ENTRADAS("Entradas"),
    SALIDAS("Salidas"),
    EN_PUERTO("En puerto");

    val lista: Lista?
        get() = when (this) {
            PRINCIPAL -> null
            ENTRADAS -> Lista.ENTRADAS
            SALIDAS -> Lista.SALIDAS
            EN_PUERTO -> Lista.EN_PUERTO
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: BuquesViewModel = viewModel()) {
    val estado by vm.estado.collectAsState()
    var seleccionado by remember { mutableStateOf<Buque?>(null) }
    var verInformacion by remember { mutableStateOf(false) }

    if (verInformacion) {
        DialogoInformacion(
            version = BuildConfig.VERSION_NAME,
            ultimaActualizacion = estado.ultimaActualizacion,
            onCerrar = { verInformacion = false }
        )
    }

    val buque = seleccionado
    if (buque != null) {
        // Al abrir la ficha se consulta VesselFinder para ese buque.
        LaunchedEffect(buque.clave) { vm.cargarVesselFinder(buque) }
        FichaVesselFinder(
            buque = buque,
            detalle = estado.detalle(buque),
            atraquePrevisto = estado.atraqueDeEscala(buque),
            onCerrar = { seleccionado = null }
        )
        return
    }

    val pantallas = Pantalla.entries
    val pager = rememberPagerState(pageCount = { pantallas.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Buques · Puerto de Santander", fontWeight = FontWeight.SemiBold)
                            Text(
                                "v${BuildConfig.VERSION_NAME}" +
                                    (estado.ultimaActualizacion?.let { " · $it" } ?: ""),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    actions = {
                        if (estado.cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp).height(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(onClick = { verInformacion = true }) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "Origen de los datos"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                ScrollableTabRow(selectedTabIndex = pager.currentPage, edgePadding = 0.dp) {
                    pantallas.forEachIndexed { i, pantalla ->
                        val n = when (pantalla) {
                            Pantalla.PRINCIPAL -> estado.movimientos.size
                            else -> estado.de(pantalla.lista!!).buques.size
                        }
                        Tab(
                            selected = pager.currentPage == i,
                            onClick = { scope.launch { pager.animateScrollToPage(i) } },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(pantalla.titulo)
                                    if (n > 0) Badge { Text(n.toString()) }
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.actualizar() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pager,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) { pagina ->
            val pantalla = pantallas[pagina]
            PullToRefreshBox(
                isRefreshing = estado.cargando,
                onRefresh = { vm.actualizar() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (pantalla == Pantalla.PRINCIPAL) {
                    PanelPrincipal(estado = estado, onBuque = { seleccionado = it })
                } else {
                    val lista = pantalla.lista!!
                    PanelLista(
                        lista = lista,
                        estadoLista = estado.de(lista),
                        estado = estado,
                        onBuque = { seleccionado = it }
                    )
                }
            }
        }
    }
}

/**
 * Pantalla principal: entradas y salidas del dia en un solo horario, en orden
 * cronologico. Los movimientos ya ocurridos quedan atenuados y el primero que
 * esta por llegar se marca como proximo.
 */
@Composable
private fun PanelPrincipal(estado: EstadoApp, onBuque: (Buque) -> Unit) {
    val ahora = LocalDateTime.now()
    val enMuelle = estado.movimientosEnMuelle
    val enFondeo = estado.movimientosEnFondeo
    val proximo = estado.proximo(ahora)
    var mostrarFondeos by remember { mutableStateOf(true) }

    val errores = listOf(Lista.ENTRADAS, Lista.SALIDAS).mapNotNull { estado.de(it).error }

    if (enMuelle.isEmpty() && enFondeo.isEmpty()) {
        Mensaje(
            if (errores.isNotEmpty()) "No se han podido cargar los datos"
            else "Sin movimientos registrados hoy",
            errores.firstOrNull() ?: "Desliza hacia abajo o pulsa el botón de actualizar"
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Cabecera(proximo?.let {
                val cual = if (it.tipo == TipoMovimiento.ENTRADA) "Entra" else "Sale"
                "$cual ${it.buque.nombre} · ${tiempoRelativo(it.momento!!, ahora)}"
            } ?: "No quedan movimientos de muelle previstos para hoy")
        }

        // --- Movimientos en los muelles -------------------------------------
        itemsIndexed(enMuelle) { indice, movimiento ->
            val pasado = movimiento.momento?.isBefore(ahora) == true
            val esProximo = proximo != null && mismoMovimiento(movimiento, proximo)

            // Separador entre lo ya ocurrido y lo que esta por venir.
            val anterior = enMuelle.getOrNull(indice - 1)
            if (esProximo && anterior?.momento?.isBefore(ahora) == true) {
                HorizontalDivider(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Box(Modifier.alpha(if (pasado) 0.55f else 1f)) {
                MovimientoCard(
                    movimiento = movimiento,
                    detalle = estado.detalle(movimiento.buque),
                    esProximo = esProximo,
                    yaPasado = pasado,
                    onClick = onBuque
                )
            }
        }

        // --- Fondeadero, fuera de la bahia ----------------------------------
        if (enFondeo.isNotEmpty()) {
            item {
                CabeceraFondeadero(
                    numero = enFondeo.size,
                    subtitulo = "Espera fuera de la bahía, sin ocupar muelle",
                    desplegada = mostrarFondeos,
                    onAlternar = { mostrarFondeos = !mostrarFondeos }
                )
            }
            if (mostrarFondeos) {
                items(enFondeo) { movimiento ->
                    val pasado = movimiento.momento?.isBefore(ahora) == true
                    Box(Modifier.alpha(if (pasado) 0.55f else 1f)) {
                        MovimientoCard(
                            movimiento = movimiento,
                            detalle = estado.detalle(movimiento.buque),
                            esProximo = false,
                            yaPasado = pasado,
                            destinoAtraque = estado.atraqueDeEscala(movimiento.buque)?.muelle,
                            onClick = onBuque
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Datos: Autoridad Portuaria de Santander · puertosantander.es",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
}

/** Dos movimientos de la misma escala, tipo y hora son el mismo movimiento. */
private fun mismoMovimiento(a: es.puertosantander.buques.data.Movimiento, b: es.puertosantander.buques.data.Movimiento) =
    a.buque.escala == b.buque.escala &&
        a.tipo == b.tipo &&
        a.horaTexto == b.horaTexto &&
        a.buque.muelle == b.buque.muelle

@Composable
private fun PanelLista(
    lista: Lista,
    estadoLista: EstadoLista,
    estado: EstadoApp,
    onBuque: (Buque) -> Unit
) {
    // El fondeadero va aparte en las tres listas: no es ocupacion de muelle.
    val enFondeo = estadoLista.buques.filter { it.esFondeo }
    val enMuelle = estadoLista.buques.filter { !it.esFondeo }
    var mostrarFondeos by remember { mutableStateOf(true) }

    when {
        estadoLista.error != null && estadoLista.buques.isEmpty() ->
            Mensaje("No se han podido cargar los datos", estadoLista.error)

        estadoLista.buques.isEmpty() && !estadoLista.cargando ->
            Mensaje(
                when (lista) {
                    Lista.ENTRADAS -> "Sin entradas previstas para hoy"
                    Lista.SALIDAS -> "Sin salidas previstas para hoy"
                    Lista.EN_PUERTO -> "No hay buques en el puerto"
                },
                "Desliza hacia abajo o pulsa el botón de actualizar"
            )

        else -> {
            val atraquesPorEscala = estadoLista.buques
                .groupingBy { it.escala }
                .eachCount()

            LazyColumn(
                contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                estadoLista.encabezado?.let { cab ->
                    item {
                        Text(
                            cab,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                if (lista == Lista.EN_PUERTO) {
                    // Agrupado por atraque, en el orden que da la web del puerto.
                    val porAtraque = enMuelle.groupBy { it.muelle.ifBlank { "Sin atraque" } }
                    porAtraque.forEach { (muelle, buques) ->
                        item { CabeceraAtraque(muelle, buques.size) }
                        items(buques) { buque ->
                            BuqueCard(
                                buque = buque,
                                lista = lista,
                                detalle = estado.detalle(buque),
                                atraquesDeLaEscala = atraquesPorEscala[buque.escala] ?: 1,
                                mostrarMuelle = false,
                                onClick = onBuque
                            )
                        }
                    }
                } else {
                    items(enMuelle) { buque ->
                        BuqueCard(
                            buque = buque,
                            lista = lista,
                            detalle = estado.detalle(buque),
                            atraquesDeLaEscala = atraquesPorEscala[buque.escala] ?: 1,
                            onClick = onBuque
                        )
                    }
                }

                if (enFondeo.isNotEmpty()) {
                    item {
                        CabeceraFondeadero(
                            numero = enFondeo.size,
                            subtitulo = when (lista) {
                                Lista.ENTRADAS -> "Buques que llegan al fondeadero"
                                Lista.SALIDAS -> "Buques que levan anclas del fondeadero"
                                Lista.EN_PUERTO -> "Buques esperando fuera de la bahía"
                            },
                            desplegada = mostrarFondeos,
                            onAlternar = { mostrarFondeos = !mostrarFondeos }
                        )
                    }
                    if (mostrarFondeos) {
                        items(enFondeo) { buque ->
                            BuqueCard(
                                buque = buque,
                                lista = lista,
                                detalle = estado.detalle(buque),
                                destinoAtraque = estado.atraqueDeEscala(buque)?.muelle,
                                onClick = onBuque
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Mensaje(titulo: String, detalle: String?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.DirectionsBoat,
                contentDescription = null,
                modifier = Modifier.height(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            detalle?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
