# NOVA Browser para Android

Navegador Android nativo basado en WebView, con interfaz oscura futurista.

## Crear el APK con Android Studio
1. Instala Android Studio.
2. Abre la carpeta `NOVA-Browser-Android`.
3. Espera a que Gradle sincronice el proyecto.
4. Ve a **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. El APK de prueba aparecerá en `app/build/outputs/apk/debug/app-debug.apk`.

## Crear el APK gratis en GitHub
1. Crea un repositorio en GitHub.
2. Sube el contenido de esta carpeta a la rama `main`.
3. Abre la pestaña **Actions** y ejecuta **Build NOVA Browser APK** (o espera al build automático).
4. En el resultado de la ejecución, descarga el artefacto `NOVA-Browser-APK`.
5. Dentro estará `app-debug.apk`.

## Notas
- `app-debug.apk` es una versión de prueba instalable, no una publicación firmada para Play Store.
- La app necesita conexión a internet para cargar sitios.
- Por seguridad, solo permite contenido web HTTPS y bloquea contenido mixto HTTP dentro de páginas HTTPS.
- WebView usa el motor del componente Android System WebView/Chrome instalado en el dispositivo.
