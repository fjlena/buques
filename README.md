# Buques · Puerto de Santander (Android)

Aplicación Android que muestra, en tres pestañas, el tráfico del Puerto de Santander leído
directamente de la web de la Autoridad Portuaria:

| Pestaña | Fuente |
|---|---|
| **Entradas** | https://www.puertosantander.es/es/entradas-hoy |
| **Salidas** | https://www.puertosantander.es/es/salidas-hoy |
| **En puerto** | https://www.puertosantander.es/es/buques-en-el-puerto |

- Las tres listas se descargan **al abrir la aplicación** (en paralelo) y se pueden recargar
  con el **botón flotante de actualizar** o **deslizando hacia abajo**.
- En "En puerto" cada buque muestra la **fecha y hora prevista de salida** en un bloque
  destacado, con el tiempo restante ("en 3 h 20 min"), y la lista se ordena por esa hora:
  primero los que se van antes.
- Al **pulsar un buque** se abre su ficha: arriba los datos de la escala según el puerto
  (muelle, atraque, salida prevista, consignatario) y debajo la **ficha de VesselFinder**
  cargada en la propia aplicación, con botón para abrirla en el navegador.

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
│   ├── Buque.kt                 modelo de datos y URLs (incl. VesselFinder)
│   ├── PuertoRepository.kt      descarga y parseo del HTML con Jsoup
│   └── BuquesViewModel.kt       carga en paralelo, orden y errores
└── ui/
    ├── BuqueCard.kt             tarjeta de buque, bandera, tiempos relativos
    ├── FichaVesselFinder.kt     ficha con WebView de VesselFinder
    └── Theme.kt                 paleta (azul puerto / ámbar para salidas)
```

Tecnología: Kotlin, Jetpack Compose (Material 3), corrutinas y **Jsoup** para el parseo.
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
- La ficha del puerto no incluye el IMO ni el MMSI del buque, así que VesselFinder se abre
  por **búsqueda por nombre**. Cuando hay una única coincidencia entra directamente en la
  ficha; con nombres muy genéricos puede aparecer primero la lista de resultados.
- Los datos son de la Autoridad Portuaria de Santander y las fichas de VesselFinder son de
  VesselFinder. La aplicación es un visor de uso personal; conviene no bajar el intervalo de
  actualización a peticiones continuas.
