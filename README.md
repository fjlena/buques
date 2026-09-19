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
  horario cronológico, con la hora a la izquierda y la etiqueta ENTRA o SALE. Los **fondeos**
  van en un bloque aparte y plegable al final, porque son espera fuera de la bahía y no
  ocupación de muelle; cada uno indica el atraque al que irá el buque después. El primer
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
- Como la ficha del puerto no da el IMO, hay que buscar en VesselFinder por nombre. Buscar solo
  por nombre confunde homónimos de tamaños muy distintos (el ferry SALAMANCA de 214 m y varios
  veleros con ese nombre), así que la aplicación **comprueba la eslora** de cada candidato
  contra la del puerto, con un margen del 8 % o 8 metros, el mayor de los dos. Abre hasta cinco
  fichas candidatas y se queda con la que cuadra. Si no puede verificar el tamaño, lo dice en
  la ficha en lugar de dar por bueno el resultado.
- Si el buque no tiene foto subida o si VesselFinder deniega la petición, la ficha lo indica y
  el resto de datos sigue estando: la aplicación no depende de esa consulta.

### Atraques repetidos: no son duplicados

La tabla del puerto no lista escalas, lista **atraques**. Un buque que fondea y luego atraca, o
que cambia de muelle, aparece varias veces el mismo día con el mismo número de escala: la
escala 1141/2026 del AUTOSKY figura dos veces en RAOS 8, y el LUCIA B aparece en FONDEO y en
RAOS 3. La aplicación lo interpreta así: dentro de cada escala, el primer atraque es la entrada
real al puerto y el último desatraque la salida real; los intermedios son maniobras internas y
se muestran en gris con la etiqueta ATRACA o DESATRACA y la nota "cambio de atraque". En las
pestañas de entradas y salidas, las tarjetas de una escala repetida avisan de cuántas veces
figura ese día.

### Fondeos

La web agrupa bajo el muelle FONDEO los buques que esperan fuera de la bahía. La aplicación los
trata como una categoría propia, con las etiquetas FONDEA y LEVA, y los saca del horario
principal: un fondeo nunca se marca como próximo movimiento y no cuenta como entrada ni salida
del puerto, de modo que el atraque posterior conserva su papel de entrada real. El destino se
deduce buscando en las tres listas otra fila con el mismo número de escala y muelle real; se
prefiere la posterior al fondeo y, cuando las horas de la web no son coherentes (a veces el
atraque figura antes que el fondeo), se toma la primera disponible. Si no hay ninguna, la ficha
dice "Sin asignar todavía" en lugar de inventar un destino.
## Fuentes y aviso legal

La aplicación incluye un botón de información (icono ⓘ en la barra superior) que muestra el
origen de los datos: **Autoridad Portuaria de Santander – Puerto de Santander**, con su
logotipo y enlaces a las tres páginas consultadas, a `puertosantander.es` y a su aviso legal;
y **VesselFinder** para la fotografía, el IMO, el MMSI y los datos AIS, con enlace a sus
condiciones de uso. Indica también la versión y la hora en que se consultaron los datos, que es
lo que suelen exigir las condiciones de reutilización de información del sector público
(Ley 37/2007): citar la fuente y la fecha, y no desnaturalizar el contenido.

El mismo diálogo advierte de que se trata de una aplicación personal, sin carácter oficial y sin
relación con ninguna de las dos entidades, y de que los datos son informativos y no deben usarse
para la navegación ni para decisiones operativas. La pantalla principal lleva además una línea
de atribución al pie, visible sin abrir el diálogo.

El logotipo **no se incluye en el APK**: se carga desde la propia web del puerto, de modo que se
muestra como atribución a la fuente sin redistribuir la marca. Si no carga, queda el nombre en
texto, que es lo que de verdad cumple la función de citar.

Conviene no bajar el intervalo de actualización a peticiones continuas: la aplicación ya pide
cada ficha técnica una sola vez por buque y limita las peticiones simultáneas.
