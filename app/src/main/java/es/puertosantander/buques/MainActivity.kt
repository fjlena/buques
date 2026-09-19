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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.puertosantander.buques.data.Buque
import es.puertosantander.buques.data.BuquesViewModel
import es.puertosantander.buques.data.EstadoLista
import es.puertosantander.buques.data.Lista
import es.puertosantander.buques.ui.BuqueCard
import es.puertosantander.buques.ui.BuquesTheme
import es.puertosantander.buques.ui.FichaVesselFinder
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: BuquesViewModel = viewModel()) {
    val estado by vm.estado.collectAsState()
    var seleccionado by remember { mutableStateOf<Buque?>(null) }

    val buque = seleccionado
    if (buque != null) {
        FichaVesselFinder(buque = buque, onCerrar = { seleccionado = null })
        return
    }

    val listas = Lista.entries
    val pager = rememberPagerState(pageCount = { listas.size })
    val scope = rememberCoroutineScopeCompat()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Buques · Puerto de Santander", fontWeight = FontWeight.SemiBold)
                            estado.ultimaActualizacion?.let {
                                Text(
                                    "Actualizado: $it",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    actions = {
                        if (estado.cargando) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 16.dp).height(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                TabRow(selectedTabIndex = pager.currentPage) {
                    listas.forEachIndexed { i, lista ->
                        val n = estado.de(lista).buques.size
                        Tab(
                            selected = pager.currentPage == i,
                            onClick = { scope.launch { pager.animateScrollToPage(i) } },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(lista.titulo)
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
            val lista = listas[pagina]
            PullToRefreshBox(
                isRefreshing = estado.de(lista).cargando,
                onRefresh = { vm.actualizar() },
                modifier = Modifier.fillMaxSize()
            ) {
                PanelLista(
                    lista = lista,
                    estadoLista = estado.de(lista),
                    onBuque = { seleccionado = it }
                )
            }
        }
    }
}

@Composable
private fun PanelLista(
    lista: Lista,
    estadoLista: EstadoLista,
    onBuque: (Buque) -> Unit
) {
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

        else -> LazyColumn(
            contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            estadoLista.encabezado?.let { cab ->
                item {
                    Text(
                        cab,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            items(estadoLista.buques) { buque ->
                BuqueCard(buque = buque, lista = lista, onClick = onBuque)
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

/** Pequeño ayudante para no importar rememberCoroutineScope en varios sitios. */
@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
