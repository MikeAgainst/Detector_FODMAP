# Detector FODMAP 🥑

**Detector FODMAP** es una aplicación Android nativa desarrollada en Java que ayuda a identificar alimentos según su contenido en FODMAP, clasificándolos en tres niveles: bajo, moderado y alto.

Está pensada para facilitar la consulta de alimentos y ayudar a interpretar las etiquetas de productos envasados mediante un buscador, fichas informativas y un lector de códigos de barras.

## Funcionalidades

### 🔎 Búsqueda de alimentos
- Buscador con tolerancia a errores de escritura, tildes y variaciones en los nombres.
- Base de datos local con 158 alimentos en formato JSON.
- Ficha individual con nivel FODMAP, ración orientativa, FODMAPs asociados y notas informativas.
- Acceso a fotografías de los alimentos mediante Wikipedia, con caché local y alternativas visuales cuando no hay conexión.

### 📷 Escáner de productos
- Lectura de códigos de barras mediante la cámara del dispositivo.
- Consulta de productos envasados a través de Open Food Facts.
- Análisis de la lista de ingredientes para detectar componentes potencialmente relevantes, como lactosa, inulina, fructosa y determinados polioles.
- Identificación de ingredientes coincidentes con la base de datos, mostrando su clasificación y permitiendo consultar sus fichas.
- Alternativa de búsqueda manual cuando un producto no está disponible en la base de datos externa.

### 🎨 Interfaz y experiencia de usuario
- Interfaz nativa para Android.
- Modos claro y oscuro.
- Icono adaptativo compatible con las distintas densidades de pantalla.
- Respaldo visual mediante emojis o iniciales cuando no se dispone de una imagen.

## Tecnologías utilizadas

| Tecnología | Uso |
|---|---|
| **Java** | Lenguaje principal de la aplicación |
| **Android SDK** | Desarrollo de la interfaz y funcionalidades nativas |
| **CameraX** | Acceso a la cámara |
| **Google ML Kit** | Reconocimiento de códigos de barras en el dispositivo |
| **Open Food Facts API** | Consulta de información de productos e ingredientes |
| **Wikipedia REST API** | Obtención de fotografías de alimentos |
| **Glide** | Carga y almacenamiento en caché de imágenes |
| **JSON** | Almacenamiento local de la información nutricional orientativa |
| **Gradle** | Configuración y compilación del proyecto |

## Privacidad

- El reconocimiento de códigos de barras se realiza en el dispositivo.
- La imagen capturada por la cámara no se guarda ni se envía a los servicios consultados.
- Al consultar un producto, se envía su código de barras a Open Food Facts.
- Las fotografías se obtienen de Wikipedia cuando se dispone de conexión a Internet.

## Limitaciones

- La información de Open Food Facts depende de los datos disponibles para cada producto y puede estar incompleta.
- El análisis de ingredientes se basa en coincidencias con la base de datos y en la detección de términos conocidos; no garantiza identificar todos los ingredientes relevantes.
- La presencia de un ingrediente no determina por sí sola la tolerancia al producto: también importan la cantidad consumida y la ración.
- La clasificación de alimentos es orientativa y no sustituye el asesoramiento de un dietista-nutricionista.

## Descargar

**Versión:** 1.2

Consulta la sección **Releases** de este repositorio para descargar el APK de la aplicación, cuando esté publicado.

## Proyecto

El proyecto está desarrollado en Java y organizado por capas para separar los datos, la lógica de análisis y la interfaz de usuario.

La información de los alimentos se mantiene en un archivo JSON local, mientras que las consultas externas se utilizan para ampliar la información de productos envasados y obtener fotografías.

## Aviso

Detector FODMAP es una herramienta informativa. No diagnostica intolerancias ni sustituye las indicaciones de un profesional sanitario.
