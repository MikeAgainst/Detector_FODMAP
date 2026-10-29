# Detector FODMAP

App Android nativa en Java que dice si un alimento es **bajo, moderado o alto en FODMAP**.
La búsqueda tolera errores de escritura («Aguaxate» encuentra «Aguacate») y cada alimento
muestra una foto de Wikipedia con respaldo en emoji/inicial si no hay red.

## Estado

| Función | Estado |
| --- | --- |
| Búsqueda tolerante a errores | ✅ |
| Ficha con nivel, ración, FODMAPs y nota | ✅ |
| Tabla local de 158 alimentos en JSON | ✅ |
| Foto por alimento (Wikipedia, con caché) | ✅ |
| Escaneo de código de barras + Open Food Facts | ✅ |
| Análisis de la lista completa de ingredientes | ✅ |
| Modo claro / oscuro | ✅ |
| Release firmado (APK 1.2) | ✅ |

## Icono

El icono es un dibujo de un detective con lupa. Se genera con `System.Drawing` a partir de
la imagen original (fondo transparente) y se reparte en:

- `mipmap-mdpi..xxxhdpi/ic_launcher.png` — icono clásico con esquinas redondeadas, para Android 7.
- `mipmap-mdpi..xxxhdpi/ic_launcher_round.png` — versión circular para lanzadores redondos.
- `drawable-mdpi..xxxhdpi/ic_launcher_foreground.png` — capa frontal del icono adaptativo
  (Android 8+), con el dibujo dentro de la zona segura de 66 dp.
- `drawable-mdpi..xxxhdpi/ic_launcher_monochrome.png` — silueta para iconos tematizados
  (Android 13+), tintada por el sistema.
- Fondo del icono adaptativo: `@color/ic_launcher_background` (#FFF3E0, crema).

`preview-icono.png` en la raíz muestra cómo queda en cada caso. Para regenerarlos:
`powershell -ExecutionPolicy Bypass -File tools\make-icon.ps1` (acepta `-Source` y `-Res`).
La imagen original viene de Pngtree: **revisa su licencia antes de publicar** (las gratuitas
suelen exigir atribución y no permiten uso comercial sin suscripción).

## Compilar

Requisitos: JDK 21, Android SDK (platform 35 + build-tools 35.0.0) y Gradle 8.11.1.
En este equipo ya están instalados en `C:\Users\lynnl\android-dev`.

```powershell
$env:JAVA_HOME="C:\Users\lynnl\android-dev\jdk21\jdk-21.0.12.1+1"
$env:ANDROID_HOME="C:\Users\lynnl\android-dev\sdk"
.\gradlew.bat assembleDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

También se puede abrir la carpeta del proyecto con Android Studio (usa el SDK que
tengas configurado; `local.properties` ya apunta al SDK local).

## Release firmado

```powershell
.\gradlew.bat assembleRelease
```

Genera `app/build/outputs/apk/release/app-release.apk`, firmado con la clave de
`keystore/detectorfodmap-release.jks` y con las credenciales de `keystore.properties`
(ambos excluidos de git). Firma v2 (Android 7+), R8 y reducción de recursos activados.

- **Haz copia de seguridad de `keystore/` y `keystore.properties`.** Si se pierden, no se
  podrá actualizar la app manteniendo la firma: habría que desinstalar e instalar de nuevo.
- Si alguna librería fallara con R8: `.\gradlew.bat assembleRelease -PnoMinify`.
- El APK de release tiene otra firma que el de debug, así que para pasarse a él hay que
  desinstalar antes (`adb uninstall com.detectorfodmap.app`).
- `app/build/outputs/mapping/release/mapping.txt` permite leer los stack traces ofuscados.

## Estructura

```
app/src/main/
├── assets/foods.json          # tabla FODMAP (fuente de verdad)
├── java/com/detectorfodmap/app/
│   ├── data/Food.java         # modelo + puntuación de búsqueda
│   ├── data/FoodRepository.java
│   ├── data/Product.java      # producto de Open Food Facts
│   ├── data/OpenFoodFactsClient.java
│   ├── data/IngredientParser.java   # trocea la etiqueta
│   ├── data/IngredientAnalysis.java # cruza ingredientes con la tabla
│   ├── ui/MainActivity.java   # buscador y listado
│   ├── ui/DetailActivity.java # ficha del alimento
│   ├── ui/ScanActivity.java   # cámara + ML Kit
│   ├── ui/ProductActivity.java# producto escaneado
│   ├── ui/FoodAdapter.java
│   ├── ui/FoodImages.java     # fotos de Wikipedia + caché
│   ├── ui/LevelStyle.java
│   ├── ui/Ui.java             # edge-to-edge
│   └── util/TextNormalizer.java # normalización + similitud Dice
└── res/layout/                # activity_main, activity_detail, item_food

app/proguard-rules.pro         # reglas de R8 del release
keystore/                      # clave de firma (NO subir a git)
keystore.properties            # contraseñas de la clave (NO subir a git)
tools/make-icon.ps1            # generador de los iconos
```

## Añadir o corregir alimentos

Todo está en `app/src/main/assets/foods.json`. Cada entrada:

```json
{
  "id": "aguacate",
  "name": "Aguacate",
  "aliases": ["palta", "avocado"],
  "category": "Fruta",
  "level": "low",
  "fodmaps": [],
  "portion": "Hasta 1/2 unidad (75 g)",
  "note": "Rico en grasas saludables. En raciones grandes aparece sorbitol.",
  "emoji": "🥑",
  "wiki": "Aguacate"
}
```

- `level`: `low`, `moderate` o `high`.
- `aliases`: sinónimos y errores frecuentes; se usan en la búsqueda.
- `fodmaps`: `Fructosa`, `Lactosa`, `Fructanos`, `GOS`, `Polioles`, `Manitol`…
- `emoji`: opcional. Se usa como respaldo de la foto; conviene Unicode ≤ 9.0 para
  que se vea en Android antiguos.
- `wiki`: opcional. Título del artículo de Wikipedia si no coincide con `name`.

## Búsqueda tolerante

1. Se normaliza el texto: minúsculas, sin tildes, sin signos y con espacios simples.
2. Se puntúa cada alimento: coincidencia exacta > empieza por > contiene > similitud
   por bigramas (coeficiente de Dice ≥ 0,65) > coincidencia de todas las palabras.
3. Los resultados se ordenan por puntuación y, a igualdad, por nombre.

No requiere internet ni librerías externas.

## Fotos

`FoodImages` consulta `https://es.wikipedia.org/api/rest_v1/page/summary/{título}`,
toma el `thumbnail` y lo carga con Glide. La URL se guarda en `SharedPreferences`,
así que cada alimento se pide una sola vez. Si falla, se mantiene el emoji o la
inicial del nombre.

## Escanear un código de barras

En la pantalla principal, **Escanear código de barras** abre la cámara (ML Kit, en el
dispositivo, sin claves ni cuotas). Al leer un EAN/UPC se consulta Open Food Facts y se
analiza la lista completa de ingredientes de la etiqueta:

1. `ScanActivity` lee el código con ML Kit + CameraX (linterna incluida).
2. `ProductActivity` pide a Open Food Facts el nombre, la marca, la cantidad, la foto y el
   texto de ingredientes (`ingredients_text_es`, `ingredients_text` o la lista estructurada).
3. `IngredientParser` trocea la etiqueta por comas, puntos y paréntesis, y limpia
   porcentajes, asteriscos y marcas de alérgenos.
4. `IngredientAnalysis` busca cada trozo en la tabla (gana la coincidencia más específica:
   «leche de almendra» bajo antes que «leche» alto) y revisa marcadores que no son
   alimentos pero sí FODMAP: inulina, fibra de achicoria, FOS/GOS, jarabes de fructosa o
   maíz, polioles (E421, E953, E965, E966), lactosa, agave…
5. Se avisa de **todos** los ingredientes altos o moderados encontrados, con su nivel, y
   se permite abrir la ficha del alimento. Debajo se muestra la etiqueta tal cual.
6. Si el producto no tiene ingredientes en Open Food Facts, se mantiene el comportamiento
   anterior: se cruza el nombre del producto con la tabla.

Privacidad: la imagen de la cámara no se guarda ni se envía; solo viaja el número de
código de barras a Open Food Facts. La app pide permiso de cámara y lo explica si se niega.

Límites: solo funciona con productos envasados y registrados. La lista de ingredientes
puede venir incompleta o en otro idioma, y la cantidad importa: un ingrediente al final de
la etiqueta suele estar en poca proporción. Por eso siempre se ofrece la búsqueda manual y
se avisa de que hay que revisar la etiqueta y la ración.

Límite de uso de Open Food Facts: la API es gratuita y abierta; se envía un User-Agent
identificando la app y no se hacen peticiones en bucle (solo al escanear o reintentar).


Opciones evaluadas, de más a menos recomendable para una app sencilla y gratuita:

1. **ML Kit Image Labeling (on-device, gratis e ilimitado)**: reconoce etiquetas
   genéricas («Food», «Fruit», «Banana»), no siempre el alimento exacto.
2. **Código de barras + Open Food Facts**: ya implementado; muy fiable en envasados.
3. **IA en la nube con visión** (Gemini/OpenAI): acierta mucho más, pero exige clave
   y tiene cuota; «gratis e ilimitado» no existe en ningún proveedor serio.

Recomendación: 2 + 1 combinados y pasar el resultado por la búsqueda tolerante.

## Aviso

La clasificación es orientativa, basada en tablas FODMAP públicas. La tolerancia
depende de la persona y de la ración. No sustituye el consejo de un
dietista-nutricionista.
