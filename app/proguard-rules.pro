# Reglas de R8 para el release.
#
# Las dependencias traen sus propias reglas (consumer rules):
#   - ML Kit barcode-scanning
#   - CameraX
#   - Glide
#   - Material / AndroidX
# Por eso aquí no hace falta casi nada. Si algún día añades una librería que use
# reflexión (Gson, Retrofit, Room...), añade aquí sus reglas.

# Conserva los nombres de las clases de las vistas porque el layout las referencia
# por nombre en el XML (ViewBinding no lo necesita, pero evita sorpresas).
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Líneas de los stack traces legibles en el reporte de Google Play.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
