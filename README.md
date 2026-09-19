# Buques · Puerto de Santander (Android)

Aplicación Android que muestra el tráfico del Puerto de Santander leído directamente de la web
de la Autoridad Portuaria, en cuatro pestañas: **Hoy**, **Entradas**, **Salidas** y **En puerto**.

| Pestaña | Fuente |
|---|---|
| **Hoy** | entradas + salidas combinadas |
| **Entradas** | https://www.puertosantander.es/es/entradas-hoy |
| **Salidas** | https://www.puertosantander.es/es/salidas-hoy |
| **En puerto** | https://www.puertosantander.es/es/buques-en-el-puerto |
| Eslora, arqueo, destino | ficha del buque en la web del puerto (`/es/buque/…`) |
| Fotografía, IMO, MMSI, tipo | https://www.vesselfinder.com |

- **Hoy** es la pantalla de inicio: mezcla las entradas y las salidas del día en un único
  horario cronológico, con la hora a la izquierda y la etiqueta ENTRA o SALE. El primer
  movimiento que aún no ha ocurrido se marca como **PRÓXIMO MOVIMIENTO** (recuadrado y con el
  tiempo que falta), los ya pasados quedan atenuados y una línea separa unos de otros. Arriba,
  un resumen de una línea: qué buque es el siguiente y en cuánto tiempo.
- Las listas se descargan **al abrir la aplicación** (en paralelo) y se pueden recargar
  con el **botón flotante de actualizar** o **deslizando hacia abajo**.
- En "En puerto" cada buque muestra la **fecha y hora prevista de salida** en un bloque
  destacado, con el tiempo restante ("en 3 h 20 min"), y la lista se ordena por esa hora:
  primero los que se van antes.
- Cada tarjeta indica la **eslora** del buque (y su arqueo bruto), tomada de la ficha del
  propio puerto.
- Al **pulsar un buque** se abre su ficha: **fotografía** del buque obtenida de VesselFinder,
  ficha técnica (eslora, manga, arqueo, tipo, año, IMO, MMSI) junto con los datos de la escala,
  y un acceso para cargar la **ficha web completa de VesselFinder** dentro de la aplicación,
  con su mapa y sus datos AIS, o abrirla en el navegador.

## Cómo obtener el APK

### Opción A — Android Studio (recomendada si lo tienes instalado)

1. Descomprime el proyecto y ábrelo con **File ▸ Open** (Android Studio Ladybug o posterior).
2. Espera a que sincronice Gradle; descargará el SDK y las dependencias que falten.
3. **Build ▸ Build App Bundle(s) / APK(s) ▸ Build APK(s)**.
4. El APK queda en `app/build/outputs/apk/release/app-release.apk` (o `debug/` si compilas debug).

También puedes conectar el móvil por USB con la depuración USB activada y pulsar *Run*.

### Opción B — Línea de comandos

Necesitas JDK 17 y el SDK de Android (variable `ANDROID_HOME`):

```bash
./gradlew assembleRelease
```

### Opción C — Sin instalar nada, compilando en GitHub

El proyecto incluye `.github/workflows/build-apk.yml`. Sube la carpeta a un repositorio de
GitHub (puede ser privado) y en la pestaña **Actions** lanza el flujo *Compilar APK*
(o simplemente haz push). Al terminar, descarga el artefacto **BuquesSantander-apk**.

### Instalación en el móvil

El APK se firma con la clave de depuración, suficiente para instalarlo tú mismo pero no para
publicarlo en Google Play. En el móvil hay que permitir la instalación de aplicaciones de
orígenes desconocidos para el gestor de archivos o el navegador desde el que lo abras.

Si algún día quisieras publicarlo o firmarlo con una clave propia, habría que generar un
keystore y cambiar el `signingConfig` de `app/build.gradle.kts`.

## Estructura

```
app/src/main/java/es/puertosantander/buques/
├── MainActivity.kt              pestañas, pager, actualizar, estados vacíos
├── data/
│   ├── Buque.kt                    modelo: escala, detalle técnico y movimiento
│   ├── PuertoRepository.kt         listas y fichas del puerto (Jsoup)
│   ├── VesselFinderRepository.kt   foto, IMO/MMSI y dimensiones
│   └── BuquesViewModel.kt          carga en paralelo, caché, orden y errores
└── ui/
    ├── BuqueCard.kt                tarjetas de buque y de movimiento
    ├── FichaVesselFinder.kt        foto, ficha técnica y WebView
    └── Theme.kt                    paleta (azul entradas / ámbar salidas)
```

Tecnología: Kotlin, Jetpack Compose (Material 3), corrutinas, **Jsoup** para el parseo y
**Coil** para cargar la fotografía.
`minSdk 26` (Android 8.0), `targetSdk 35`. Sin dependencias de servicios de Google.

## Notas sobre la obtención de datos

- La web del puerto no publica una API, así que la aplicación lee la tabla HTML. El parseo
  **no depende de las clases CSS** de Drupal: localiza la tabla que contiene enlaces a
  `/es/buque/...` y reconoce cada dato por su forma (registro `nnnn/aaaa`, bandera de dos
  letras, fechas `dd/mm/aaaa - hh:mm`). Así aguanta mejor un rediseño de la web.
- Las filas de agrupación (FONDEO, RAOS 3, ASTANDER…) se interpretan como el **muelle** de los
  buques que vienen debajo.
- Hay un test que valida el parseo con una tabla de ejemplo: `./gradlew test`. Si el puerto
  cambia el formato y la app deja de mostrar datos, ese test es el punto por donde empezar.
- **Eslora**: sale del campo *Longitud* de la ficha del puerto. Se pide una sola vez por buque
  (los datos técnicos no cambian) y con un máximo de tres peticiones simultáneas, para no
  castigar al servidor. Se cachea en memoria durante la sesión.
- **Fotografía, IMO y MMSI**: se consultan en VesselFinder **solo al abrir la ficha** de un
  buque, nunca para la lista entera: serían decenas de peticiones a un servidor ajeno en cada
  actualización. Por eso la foto no aparece en las tarjetas de la lista.
- Como la ficha del puerto no da el IMO, hay que buscar en VesselFinder por nombre y entrar en
  el primer resultado. Si el nombre es ambiguo, si el buque no tiene foto subida o si
  VesselFinder deniega la petición, la ficha lo indica y el resto de datos sigue estando: la
  aplicación no depende de esa consulta.
- Los datos son de la Autoridad Portuaria de Santander y las fichas de VesselFinder son de
  VesselFinder. La aplicación es un visor de uso personal; conviene no bajar el intervalo de
  actualización a peticiones continuas.
