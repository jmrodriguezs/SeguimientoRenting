# Guía paso a paso para publicar en Google Play Console

Esta guía contiene todas las instrucciones y respuestas exactas a los cuestionarios obligatorios de Google Play Console para publicar **Seguimiento Renting**.

---

## 1. Archivo para subir (App Bundle)

El archivo generado y firmado listo para subir es:
📁 **`app/build/outputs/bundle/release/app-release.aab`**

- **Firmado con:** `release-upload.jks` (válido hasta septiembre de 2056).
- **Versión:** `versionCode = 3`, `versionName = "1.2"`.
- **Target SDK:** 36 (Android 16, cumple de sobra los requisitos de Google Play).
- **Permisos:** Solo `INTERNET`, `POST_NOTIFICATIONS` y `RECEIVE_BOOT_COMPLETED`. (Se eliminó el permiso sensible `CALL_PHONE`, evitando rechazos automáticos de Google).

---

## 2. Crear la Aplicación en Play Console

1. Inicia sesión en [Google Play Console](https://play.google.com/console).
2. Pulsa en **Crear aplicación** (arriba a la derecha).
3. Rellena los campos iniciales:
   - **Nombre de la aplicación:** `Seguimiento Renting`
   - **Idioma predeterminado:** `Español (España) – es-ES`
   - **¿Es una aplicación o un juego?:** `Aplicación`
   - **¿Es gratis o de pago?:** `Gratis`
   - **Declaraciones:** Marca las casillas de aceptación de las políticas del programa y leyes de exportación de EE.UU.
4. Pulsa en **Crear aplicación**.

---

## 3. Configuración de la Ficha de Play Store (Presencia en la tienda)

Ve a **Crecimiento > Presencia en Google Play Store > Ficha principal de la tienda**.

### Textos
- **Nombre de la aplicación (máx. 30 car.):**
  ```text
  Seguimiento Renting
  ```
- **Descripción breve (máx. 80 car.):**
  ```text
  Control de kilómetros, repostajes, gastos, proyección y liquidación de renting.
  ```
- **Descripción completa (máx. 4000 car.):**
  ```text
  Seguimiento Renting es la herramienta definitiva para llevar el control exhaustivo de tu vehículo en renting o uso personal.

  Funciones principales:
  • Mediciones de kilometraje: Registro de lecturas del cuentakilómetros con fecha y foto de control. Cálculo automático de la desviación frente a los km teóricos del contrato.
  • Proyección a fin de contrato: Escenarios por ritmo acumulado, últimos 6 meses o valor manual, con gráfica comparativa frente a la recta teórica.
  • Liquidación y costes: Estimación del abono o cargo previsto por exceso o defecto de kilómetros, cuotas mensuales, cuota irregular, coste por km real y desglose de IVA deducible.
  • Repostajes con lectura inteligente (OCR): Haz una foto al tique de combustible y la app completará automáticamente la fecha, importe, litros y precio en el propio teléfono, sin enviar la imagen a ningún servidor.
  • Precios de mercado: Consulta de precios medios de referencia del combustible según datos abiertos oficiales del Ministerio.
  • Otros gastos: Registro de peajes, aparcamiento, lavados, neumáticos y mantenimiento.
  • Consulta de multas: Búsqueda directa de la matrícula en el Tablón Edictal Único del BOE con avisos de publicaciones pendientes.
  • Informes y exportación: Generación de informe ejecutivo en PDF de 1 página para imprimir o compartir, y exportación completa a Excel (.xlsx) con fórmulas vivas.
  • Copias de seguridad: Respaldo completo en un único archivo ZIP (datos y fotos) y compatibilidad con la copia automática de Android.
  • 100% Privada y sin conexión: Los datos se almacenan exclusivamente en tu dispositivo. Sin cuentas, sin registro y sin anuncios.
  ```

### Elementos gráficos (Assets listos en la carpeta `play_store_assets/`)
- **Icono de la aplicación (512 × 512 px):**
  Sube `play_store_assets/icon_512.png`
- **Gráfico de funciones (1024 × 500 px):**
  Sube `play_store_assets/feature_graphic_1024x500.png`
- **Capturas de pantalla de teléfono:**
  Sube las imágenes de la carpeta `play_store_assets/screenshots/`:
  1. `01_resumen_principal.png`
  2. `02_resumen_indicadores.png`
  3. `03_historico_kilometros.png`
  4. `04_repostajes_y_consumo.png`
  5. `05_lectura_ocr_tique.png`
  6. `06_grafica_proyeccion.png`
  7. `07_consulta_multas_boe.png`
  8. `08_modo_oscuro.png`
  9. `09_widget_escritorio.png`

---

## 4. Respuestas a los Cuestionarios Obligatorios (Contenido de la aplicación)

En el menú lateral, ve a **Política y programas > Contenido de la aplicación**. Debes completar cada sección:

### 1. Política de Privacidad
- **URL de la política de privacidad:**
  ```text
  https://manursan2026.github.io/seguimiento-renting/politica-de-privacidad.html
  ```
  *(O la URL donde tengas publicado el archivo `docs/politica-de-privacidad.html` en tu servidor o GitHub Pages).*

### 2. Acceso a aplicaciones
- Selecciona: **"Todas las funciones están disponibles sin restricciones"** (la app no requiere login ni contraseñas).

### 3. Anuncios
- Selecciona: **"No, mi aplicación no contiene anuncios"**.

### 4. Clasificación de contenido (Cuestionario IARC)
- Introduce tu correo electrónico.
- Categoría: **"Utilidad, Productividad, Comunicación u Otro"**.
- Responde **NO** a todas las preguntas de violencia, contenido sexual, lenguaje ofensivo, sustancias controladas, apuestas, etc.
- Resultado: **PEGI 3** (Apta para todos los públicos).

### 5. Público objetivo y contenido
- Grupo de edad: Marca **18 años o más**.
- ¿Tu aplicación puede atraer de forma involuntaria a menores?: Selecciona **No**.

### 6. Aplicaciones de noticias
- Selecciona: **"No"** (no es una aplicación de noticias).

### 7. Rastreo de contactos y estado respecto a la COVID-19
- Selecciona: **"Mi aplicación no es una aplicación de rastreo ni estado respecto a la COVID-19"**.

### 8. Seguridad de los datos (Data Safety) — ¡MUY IMPORTANTE!
- ¿Tu aplicación recoge o comparte alguno de los tipos de datos de usuario requeridos?:
  👉 Selecciona **"NO"**.
  *(Explicación: Toda la información que el usuario introduce —km, repostajes, fotos— se guarda únicamente de forma local en el dispositivo del usuario; no se envía a ningún servidor propio ni externo).*
- ¿Todos los datos de usuario recogidos por tu aplicación se cifran en tránsito?:
  *(Si te lo pregunta, selecciona Sí; las consultas al BOE y al Ministerio van por HTTPS).*

### 9. Identificador publicitario (Advertising ID)
- ¿Tu aplicación utiliza el ID de publicidad?:
  👉 Selecciona **"NO"**.

### 10. Funciones financieras (Financial Features)
- Si Google te pide declarar funciones financieras:
  👉 Selecciona que es una **herramienta de cálculo y registro para uso personal** y que **no presta servicios bancarios, créditos, pagos ni préstamos**.

---

## 5. Lanzar la versión

1. En el menú lateral, ve a:
   - Si tu cuenta es de desarrollador personal creada después de nov. 2023: Ve a **Pruebas > Pruebas cerradas** (Google exige 20 testers durante 14 días antes de pasar a producción).
   - Si tu cuenta es de organización o antigua: Ve a **Versión > Producción**.
2. Pulsa en **Crear nueva versión**.
3. En **Firma de aplicaciones de Play**, pulsa en Continuar / Aceptar (Play App Signing utilizará la clave de subida `release-upload.jks` que configuramos).
4. En **Paquetes de aplicaciones**, arrastra y suelta el archivo:
   `app/build/outputs/bundle/release/app-release.aab`
5. Nombre de la versión: `1.2 (3)`.
6. En **Notas de la versión**, añade:
   ```text
   Versión inicial de Seguimiento Renting:
   - Control de kilómetros, previsión a fin de contrato y liquidación estimada.
   - Historial de repostajes y consumo con lectura automática de tiques por OCR.
   - Consulta de multas en el BOE y precios de referencia de combustible.
   - Widget para pantalla de inicio y exportación de informe PDF y hoja Excel.
   ```
7. Pulsa en **Revisar versión** y luego en **Iniciar lanzamiento**.

---

## 6. Información del Almacén de Claves (Keystore)

El almacén de claves release generado es:
- **Ruta:** `release-upload.jks`
- **Alias:** `seguimientokm`
- **Contraseña:** `SeguimientoRenting2026!`
- **Huella SHA-256:** `96:19:5B:EE:0A:C6:04:D3:A2:13:71:D7:4B:20:BF:F7:B0:B3:45:19:40:25:AC:EC:FB:0F:41:04:DA:DC:B0:7A`

> ⚠️ **IMPORTANTE:** Guarda una copia segura de `release-upload.jks` y `keystore.properties` fuera del repositorio (por ejemplo en un gestor de contraseñas seguro o Drive personal). Si pierdes esta clave, tendrías que solicitar a Google restablecer la clave de subida.
