# 🚀 Manual: Subir Versión, Compilar y Publicar en Google Play Console

Este manual detalla los pasos exactos y buenas prácticas para actualizar la versión de **Seguimiento Renting**, compilar el paquete firmado (`.aab`) y subirlo a **Google Play Console**.

---

## 📌 1. Conceptos Fundamentales de Versión en Android

En Android existen dos campos de versión dentro de [`app/build.gradle.kts`](app/build.gradle.kts):

| Parámetro | Tipo | Ejemplo actual | Función |
| :--- | :--- | :--- | :--- |
| **`versionCode`** | Número entero (`Int`) | `5` | **Uso interno de Google Play.** Debe incrementarse obligatoriamente en **+1** con cada nueva subida a la consola. Google Play rechazará cualquier subida con un `versionCode` igual o menor al ya existente. |
| **`versionName`** | Cadena de texto (`String`) | `"1.2.2"` | **Nombre visible para los usuarios** en la Play Store. Sigue la convención de versionado semántico (`MAJOR.MINOR.PATCH`). |

### 💡 Guía rápida para `versionName`:
- **Parche (ej. `1.2.2` ➔ `1.2.3`):** Correcciones de errores menores, textos o ajustes cosméticos.
- **Menor (ej. `1.2.3` ➔ `1.3.0`):** Nuevas funcionalidades o mejoras relevantes sin romper compatibilidad.
- **Mayor (ej. `1.3.0` ➔ `2.0.0`):** Rediseño completo, grandes cambios estructurales o cambios de contrato de datos.

---

## 🛠️ 2. Paso a Paso: Subir la Versión en el Código

1. Abre el archivo [`app/build.gradle.kts`](app/build.gradle.kts).
2. Localiza el bloque `defaultConfig` (aproximadamente en las líneas 13–22):

   ```kotlin
   defaultConfig {
       applicationId = "com.manursan.seguimientokm"
       testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       minSdk = 26
       targetSdk = 36
       versionCode = 7        // ⬅️ Incrementa en +1 respecto al anterior
       versionName = "1.2.4"  // ⬅️ Actualiza el texto visible
       ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
   }
   ```

3. Guarda los cambios:
   - Si estás en **Android Studio**, pulsa en la barra superior el botón **"Sync Now"** (sincronizar Gradle).
   - Si estás en **VS Code / Cursor**, simplemente guarda el archivo (`Ctrl + S`).

---

## 🔑 3. Verificación de la Clave de Firma

El proyecto ya está configurado para firmar de forma automática en modo release siempre que existan los siguientes dos archivos en la raíz del proyecto:

1. **`keystore.properties`**: Contiene los alias y contraseñas de firma (ignorado en Git por seguridad).
2. **`release-upload.jks`**: El almacén de claves (keystore) generado para Google Play.

*(Ambos archivos ya están en la raíz de tu proyecto, por lo que la firma es 100% automática al compilar).*

---

## 📦 4. Compilar el Paquete para Producción (.aab)

Google Play exige el formato **Android App Bundle (`.aab`)** en lugar de APK para producción.

### Opción A: Desde Terminal / PowerShell (Recomendado y más rápido)

Abre una terminal de PowerShell en la raíz del proyecto y ejecuta:

```powershell
.\gradlew.bat clean bundleRelease
```

> **¿Por qué `clean`?**  
> `clean` borra la caché de compilaciones previas en `app/build/`, garantizando que el nuevo paquete no arrastre recursos obsoletos.

Una vez terminado con éxito, verás:
```text
BUILD SUCCESSFUL in Xs
```

### Opción B: Desde la interfaz de Android Studio

1. En el menú superior, ve a **Build > Generate Signed Bundle / APK...**
2. Selecciona **Android App Bundle** y pulsa **Next**.
3. Rellena los datos de la clave:
   - **Key store path:** Selecciona `release-upload.jks` en la raíz del proyecto.
   - **Key store password:** `SeguimientoRenting2026!`
   - **Key alias:** `seguimientokm`
   - **Key password:** `SeguimientoRenting2026!`
4. Pulsa **Next**, selecciona la variante **`release`** y pulsa **Create**.

---

## 📍 5. Dónde encontrar el archivo generado

El archivo generado y firmado para subir a Google Play se guardará exactamente en:

```text
📁 app/build/outputs/bundle/release/app-release.aab
```

### ⚡ Acceso rápido desde PowerShell:
Para abrir directamente la carpeta en el Explorador de archivos de Windows:
```powershell
explorer app\build\outputs\bundle\release
```

---

## 🌐 6. Subir la Versión a Google Play Console

1. Entra en [Google Play Console](https://play.google.com/console) con tu cuenta de desarrollador.
2. Selecciona la app: **Seguimiento Renting**.
3. En el menú lateral izquierdo, ve al canal donde vas a publicar:
   - **Producción** (en la sección *Lanzamiento* / *Producción*), o
   - **Pruebas cerradas** (si tu cuenta está en la fase de pruebas con testers).
4. Arriba a la derecha, pulsa en el botón azul **Crear nueva versión**.
5. En la sección **Paquetes de aplicaciones**:
   - Arrastra y suelta el archivo `app-release.aab` (o pulsa *Subir* y selecciónalo).
   - Verás cómo se procesa y automáticamente Play Console indicará la nueva versión (ej. `1.2.3 (6)`).
6. **Nombre de la versión:** Se completará automáticamente como `1.2.3 (6)` (puedes ajustarlo si lo deseas).
7. **Notas de la versión:** Escribe un breve resumen de los cambios para los usuarios. Por ejemplo:
   ```text
   Versión 1.2.3:
   - Correcciones menores y optimizaciones de rendimiento.
   - Mejoras en la precisión del cálculo de proyección de kilometraje.
   ```
8. Pulsa en **Siguiente** o **Revisar versión** (abajo a la derecha).
9. Revisa que no haya errores bloqueantes en rojo:
   - *(Las advertencias en amarillo sobre archivos de símbolos de depuración o desofuscación ProGuard son opcionales y no impiden publicar)*.
10. Pulsa en **Guardar** y luego en:
    - **Iniciar lanzamiento a producción** (o a pruebas cerradas).
    - Si tienes activada la **Publicación administrada**: Ve a la pestaña **Vista general de la publicación** y pulsa en **Enviar para revisión**.

---

## 🏷️ 7. Registrar la Versión en Git (Buena Práctica)

Para mantener un historial impecable en tu repositorio de GitHub, guarda un commit y crea una etiqueta (tag) para la versión lanzada:

```powershell
# 1. Añadir los cambios del archivo de configuración
git add app/build.gradle.kts

# 2. Hacer commit de la subida de versión
git commit -m "chore: bump version to 1.2.3 (6)"

# 3. Crear una etiqueta anotada para la versión
git tag -a v1.2.3 -m "Release v1.2.3"

# 4. Subir el commit y la etiqueta a GitHub
git push origin main --tags
```

---

## ❓ Preguntas Frecuentes y Errores Comunes

### Error: *"Ya has subido un archivo APK o Android App Bundle con el código de versión X..."*
- **Causa:** Olvidaste subir el `versionCode` en `app/build.gradle.kts` o pusiste un número igual/menor a una versión que ya subiste previamente (incluso si fue en un canal de pruebas).
- **Solución:** Sube `versionCode` al siguiente número entero disponible (ej. si estaba en `5`, pon `6`), vuelve a compilar con `.\gradlew.bat clean bundleRelease` y sube el nuevo `.aab`.

### Error: *"El paquete de aplicaciones no está firmado..."* o clave incorrecta
- **Causa:** No se encontró el archivo `keystore.properties` o `release-upload.jks`.
- **Solución:** Asegúrate de no haber borrado ni renombrado `keystore.properties` ni `release-upload.jks` en la raíz del proyecto.

### ¿Se debe subir un APK o un AAB?
- **Siempre AAB (`.aab`):** Google Play exige el formato App Bundle para todas las aplicaciones nuevas y actualizaciones. El bundle permite a Google optimizar el tamaño de descarga para cada dispositivo concreto.
