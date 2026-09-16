# Seguimiento Renting — app Android del SEGUIMIENTO_KILOMETROS.xlsx

App nativa (Kotlin + Jetpack Compose, Material 3, Android 8.0+) que replica la hoja de cálculo
de seguimiento del renting: mediciones de kilómetros, repostajes, proyección a fin de contrato,
liquidación por km y coste total con IVA. Los datos se guardan en el propio teléfono.

## Instalar el APK en el móvil
1. Copia `SeguimientoKm.apk` al teléfono (cable, AirDrop/Nearby, Drive, correo…).
2. Ábrelo desde el explorador de archivos. Android pedirá permitir "instalar apps desconocidas"
   para esa app (Archivos, Chrome…): acéptalo una vez.
3. Al abrirla por primera vez está vacía: introduce los datos del contrato en Ajustes o restaura una copia de seguridad desde el menú ⋮.

El APK está firmado con la clave de depuración: sirve para uso personal, pero no para Google Play.

## Funciones
- Histórico de kilómetros y repostajes con todos los cálculos del Excel (teóricos, desviación, coste/km, proyección, liquidación, coste total, IVA).
- **¿Cuánto puedo conducir?**: km/día y km/mes máximos para no pagar exceso (30.000 km) y para conservar el abono (< 27.000 km).
- **Gráfica** de km reales frente a la recta teórica, con la proyección y los umbrales.
- **Escenarios de proyección**: media acumulada, ritmo de los últimos 6 meses o valor manual (palanca N22 del Excel).
- **Litros y precio/litro** en cada repostaje, manual o **precio medio de mercado** (Gasolina 95, datos abiertos del
  Ministerio de Industria, por fecha y provincia). Consumo en l/100 km y precio medio. "Completar precios de mercado"
  rellena los repostajes antiguos.
- **Otros gastos** (peajes, parking, lavado, multas, mantenimiento, AdBlue): entran en el coste por km y en el coste de uso hasta hoy.
- **Foto del cuentakilómetros** en cada medición (cámara o galería), reducida a 1600 px.
- **Recordatorios**: notificación mensual para anotar los km y aviso 30 días antes del ajuste anual.
- **Próximo ajuste anual**: fecha, km previstos, desviación y si cae dentro de la banda ±10 %.
- **Widget** de pantalla de inicio con km actuales, desviación y margen diario.
- Cuenta atrás de fin de contrato, modo claro/oscuro, copia de seguridad en ZIP (datos + fotos) y copia automática de Android.

## Pantallas
- **Resumen**: estado actual (km, desviación vs. teóricos, km/día, coste/km), combustible,
  liquidación estimada, coste total del contrato e IVA deducible.
- **Kilómetros**: histórico de mediciones (columnas A–H del Excel). `+` para añadir; tocar una
  tarjeta para editar o eliminar.
- **Repostajes**: histórico de repostajes (columnas I–L) con litros y precio/litro, y sección **Otros gastos**.
- **Proyección**: escenario de km/día (N22): media real acumulada o valor manual, y tabla mensual
  hasta el fin de contrato.
- **Ajustes**: apariencia, contacto de la compañía de renting (configurable; llamada directa y correo), consulta de multas (tablón del BOE con la matrícula y sede DGT), recordatorios, provincia para el precio de mercado, parámetros del contrato (M1:N15, IVA y % deducción) y borrado de todos los datos (contrato incluido), habilitado solo con una copia de seguridad de los datos actuales.
- Menú `⋮`:
  - **Informe PDF** (se abre la hoja de compartir: WhatsApp, correo, Drive, imprimir…): estado en una página A4 — indicadores, gráfica, margen, liquidación,
    combustible y gastos, coste del contrato y últimas mediciones. Generado con `PdfDocument`, sin librerías.
  - **Exportar a Excel (.xlsx)**: genera un libro con la misma disposición y las mismas fórmulas
    que `SEGUIMIENTO_KILOMETROS.xlsx` (tabla A:H, repostajes I:L, parámetros M:N, notas), con los
    rangos adaptados al número de filas. Se recalcula al abrirlo en Excel, Numbers o Google Sheets.
  - Completar precios de mercado (rellena el precio/litro de los repostajes que no lo tienen).
  - Guardar / restaurar copia de seguridad (ZIP con datos y fotos; también admite el JSON antiguo).

## Datos
Se guardan en el almacenamiento privado de la app. **Actualizar la app instalando un APK nuevo
conserva los datos**; desinstalarla los borra. Antes de cambiar de móvil, usa "Guardar copia de
seguridad" y restáurala en el nuevo.

## Compilar desde el código
Abrir la carpeta `SeguimientoKmApp` en Android Studio, o desde terminal:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest assembleRelease
```

El APK queda en `app/build/outputs/apk/release/app-release.apk`. Los tests unitarios (`CalcTest` y compañía)
comprueban el motor de cálculo contra los valores exactos de la hoja Excel; `connectedDebugAndroidTest` ejecuta
en un emulador las pruebas de notificación, widget, fotos, copia ZIP y consulta real del precio de mercado.

## Licencia
Este proyecto se distribuye bajo la licencia MIT (ver [LICENSE](LICENSE)): puede usarse, modificarse,
redistribuirse y publicarse (por ejemplo en Google Play) libremente, siempre que se conserve el aviso de copyright.

## Publicación en Google Play
- Generar un App Bundle con `./gradlew bundleRelease` (Play exige `.aab`, no APK).
- Firmar con una clave propia (keystore) configurada en `app/build.gradle.kts` o usar Play App Signing; nunca subir la clave al repositorio.
- La app no envía datos a ningún servidor: solo consulta el precio del combustible (datos abiertos del Ministerio) y el BOE.
