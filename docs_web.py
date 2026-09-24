import json, os, sys, datetime
# Uso: docs_web.py            -> docs/index.html con enlaces a las releases de GitHub (GitHub Pages)
#      docs_web.py --local    -> ../Web/index.html con enlaces relativos (APK y PDF en la misma carpeta)
LOCAL = "--local" in sys.argv
import re
VERSION = re.search(r'versionName = "([^"]+)"', open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "app", "build.gradle.kts")).read()).group(1)
REL = "https://github.com/manursan2026/seguimiento-renting/releases/latest/download/"
APK_HREF = "SeguimientoRenting.apk" if LOCAL else REL + "SeguimientoRenting.apk"
PDF_HREF = "Manual_Seguimiento_Renting.pdf" if LOCAL else REL + "Manual_Seguimiento_Renting.pdf"
imgs = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "docs_web_imgs.json")))
apk = os.path.getsize("/Users/manursan/Documents/PERSONALES/RENTING_PEUGEOT/SeguimientoRenting.apk") / 1e6
pdf = os.path.getsize("/Users/manursan/Documents/PERSONALES/RENTING_PEUGEOT/Manual_Seguimiento_Renting.pdf") / 1e6
fecha = datetime.date.today().strftime("%d/%m/%Y")

# Enlaces del pie solo en la versión de GitHub Pages; la copia local no los lleva
enlaces = '<br><a href="https://jmrodriguezs.github.io/SeguimientoRenting/politica-de-privacidad.html" target="_blank" rel="noopener">Política de privacidad</a>' if LOCAL else '<br>Código fuente en <a href="https://github.com/manursan2026/seguimiento-renting">GitHub</a> · <a href="https://github.com/manursan2026/seguimiento-renting/releases">Todas las versiones</a> · <a href="politica-de-privacidad.html">Política de Privacidad</a>'

html = f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Seguimiento Renting · App Android</title>
<meta name="description" content="Aplicación Android para el seguimiento de un contrato de renting: kilómetros, combustible, gastos, proyección y liquidación.">
<style>
  :root {{
    --azul:#2962FF; --teal:#00BFA5; --naranja:#FF6D00; --rosa:#E91E63; --morado:#7C4DFF; --amarillo:#FFC857;
    --texto:#1F2937; --gris:#6B7280; --fondo:#F5F7FB; --tarjeta:#FFFFFF; --linea:#E5E7EB;
  }}
  @media (prefers-color-scheme: dark) {{
    :root {{ --texto:#F3F4F6; --gris:#9CA3AF; --fondo:#0F172A; --tarjeta:#1E293B; --linea:#334155; }}
  }}
  * {{ box-sizing:border-box; }}
  body {{ margin:0; font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif; color:var(--texto); background:var(--fondo); line-height:1.55; }}
  a {{ color:var(--azul); }}
  .wrap {{ max-width:1040px; margin:0 auto; padding:0 20px; }}
  header {{ background:linear-gradient(135deg,var(--azul) 0%,#1E88E5 60%,var(--teal) 100%); color:#fff; padding:56px 0 48px; position:relative; overflow:hidden; }}
  header .wrap {{ display:flex; gap:32px; align-items:center; flex-wrap:wrap; }}
  header h1 {{ font-size:2.6rem; margin:0 0 6px; letter-spacing:-.5px; }}
  header p.lead {{ font-size:1.15rem; margin:0 0 22px; opacity:.95; max-width:560px; }}
  .logo {{ width:96px; height:96px; flex:none; }}
  .botones {{ display:flex; gap:12px; flex-wrap:wrap; }}
  .btn {{ display:inline-flex; align-items:center; gap:10px; padding:14px 22px; border-radius:14px; font-weight:700; text-decoration:none; font-size:1.05rem; box-shadow:0 6px 18px rgba(0,0,0,.18); transition:transform .12s; }}
  .btn:hover {{ transform:translateY(-2px); }}
  .btn.apk {{ background:var(--amarillo); color:#1F2937; }}
  .btn.pdf {{ background:#fff; color:var(--azul); }}
  .btn small {{ display:block; font-weight:400; font-size:.8rem; opacity:.75; }}
  .btn svg {{ width:26px; height:26px; flex:none; }}
  main section {{ padding:44px 0; border-bottom:1px solid var(--linea); }}
  h2 {{ font-size:1.7rem; margin:0 0 8px; }}
  h2 span {{ display:inline-block; width:10px; height:26px; border-radius:4px; vertical-align:-4px; margin-right:10px; }}
  .sub {{ color:var(--gris); margin:0 0 24px; }}
  .grid {{ display:grid; grid-template-columns:repeat(auto-fit,minmax(260px,1fr)); gap:18px; }}
  .card {{ background:var(--tarjeta); border-radius:16px; padding:20px 22px; border:1px solid var(--linea); box-shadow:0 2px 8px rgba(0,0,0,.04); }}
  .card h3 {{ margin:0 0 6px; font-size:1.1rem; display:flex; align-items:center; gap:10px; }}
  .card h3 i {{ width:34px; height:34px; border-radius:10px; display:inline-flex; align-items:center; justify-content:center; font-style:normal; font-size:1.1rem; color:#fff; flex:none; }}
  .card p {{ margin:0; color:var(--gris); font-size:.97rem; }}
  .shots {{ display:flex; gap:18px; overflow-x:auto; padding:6px 2px 14px; scroll-snap-type:x mandatory; }}
  .shots figure {{ margin:0; flex:none; width:210px; scroll-snap-align:start; text-align:center; }}
  .shots img {{ width:100%; border-radius:18px; border:1px solid var(--linea); box-shadow:0 8px 24px rgba(0,0,0,.12); }}
  .shots figcaption {{ font-size:.85rem; color:var(--gris); margin-top:8px; }}
  ol.pasos {{ padding-left:0; list-style:none; counter-reset:p; display:grid; gap:12px; }}
  ol.pasos li {{ counter-increment:p; background:var(--tarjeta); border:1px solid var(--linea); border-radius:14px; padding:14px 16px 14px 60px; position:relative; }}
  ol.pasos li::before {{ content:counter(p); position:absolute; left:16px; top:12px; width:30px; height:30px; border-radius:50%; background:var(--azul); color:#fff; font-weight:700; display:flex; align-items:center; justify-content:center; }}
  .dl {{ display:grid; grid-template-columns:repeat(auto-fit,minmax(280px,1fr)); gap:18px; }}
  .dl .card {{ display:flex; flex-direction:column; gap:10px; }}
  .dl .btn {{ align-self:flex-start; box-shadow:none; border:2px solid var(--linea); }}
  .dl .btn.apk {{ border-color:transparent; }}
  .dl .btn.pdf {{ background:var(--azul); color:#fff; border-color:transparent; }}
  .nota {{ background:rgba(255,200,87,.18); border-left:4px solid var(--amarillo); padding:12px 16px; border-radius:8px; font-size:.95rem; }}
  footer {{ padding:28px 0 40px; color:var(--gris); font-size:.9rem; text-align:center; }}
  .req {{ display:flex; gap:10px; flex-wrap:wrap; margin-top:6px; }}
  .req span {{ background:var(--fondo); border:1px solid var(--linea); border-radius:999px; padding:4px 12px; font-size:.85rem; }}
</style>
</head>
<body>

<header>
  <div class="wrap">
    <svg class="logo" viewBox="0 0 100 100" aria-hidden="true">
      <circle cx="50" cy="50" r="46" fill="rgba(255,255,255,.14)"/>
      <path d="M22 66 A32 32 0 1 1 78 66" fill="none" stroke="#fff" stroke-width="7" stroke-linecap="round"/>
      <line x1="50" y1="56" x2="70" y2="36" stroke="#FFC857" stroke-width="6" stroke-linecap="round"/>
      <circle cx="50" cy="56" r="6" fill="#FFC857"/>
    </svg>
    <div>
      <h1>Seguimiento Renting</h1>
      <p class="lead">Aplicación Android para controlar tu contrato de renting: kilómetros, combustible, gastos, proyección a fin de contrato y liquidación estimada.</p>
      <div class="botones">
        <a class="btn apk" href="{APK_HREF}" download>
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M17.6 9.48l1.84-3.18c.16-.31.04-.69-.26-.85-.29-.15-.65-.06-.83.22l-1.88 3.24a11.4 11.4 0 0 0-8.94 0L5.65 5.67a.63.63 0 0 0-.87-.2c-.28.18-.37.54-.22.83L6.4 9.48A10.8 10.8 0 0 0 1 18h22a10.8 10.8 0 0 0-5.4-8.52zM7 15.25a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5zm10 0a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5z"/></svg>
          <span>Descargar APK<small>Android 8.0 o superior · {apk:.1f} MB</small></span>
        </a>
        <a class="btn pdf" href="{PDF_HREF}" download>
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm-1 7V3.5L18.5 9H13zM8 13h8v2H8v-2zm0 4h8v2H8v-2z"/></svg>
          <span>Manual de usuario<small>PDF · {pdf:.1f} MB</small></span>
        </a>
      </div>
    </div>
  </div>
</header>

<main>
<div class="wrap">

<section id="que-es">
  <h2><span style="background:var(--azul)"></span>¿Qué es Seguimiento Renting?</h2>
  <p class="sub">Una app sencilla, sin cuentas ni publicidad, que guarda todos los datos en tu propio dispositivo.</p>
  <p>Los contratos de renting fijan un número de kilómetros anuales y penalizan el exceso (o abonan los kilómetros no recorridos) al finalizar. <strong>Seguimiento Renting</strong> te dice en todo momento si vas por encima o por debajo del ritmo contratado, cuántos kilómetros puedes conducir sin penalización, cuál será la liquidación estimada al final y cuánto te cuesta realmente cada kilómetro, combinando la cuota, el combustible y otros gastos.</p>
  <p>Está pensada para cualquier empresa de renting: los parámetros del contrato (kilómetros, cuota, precios por km de exceso y de abono, umbrales de liquidación, etc.) se configuran una sola vez en Ajustes.</p>
</section>

<section id="capturas">
  <h2><span style="background:var(--teal)"></span>Así se ve</h2>
  <p class="sub">Capturas de la aplicación con datos ficticios de ejemplo.</p>
  <div class="shots">
    <figure><img src="{imgs['01_resumen_1']}" alt="Pantalla Resumen"><figcaption>Resumen del estado</figcaption></figure>
    <figure><img src="{imgs['30_proy_1']}" alt="Pantalla Proyección"><figcaption>Proyección a fin de contrato</figcaption></figure>
    <figure><img src="{imgs['20_rep_lista']}" alt="Pantalla Repostajes"><figcaption>Repostajes y gastos</figcaption></figure>
    <figure><img src="{imgs['47_ajustes_multas']}" alt="Consulta de multas"><figcaption>Consulta de multas (BOE)</figcaption></figure>
    <figure><img src="{imgs['60_oscuro']}" alt="Modo oscuro"><figcaption>Modo oscuro</figcaption></figure>
  </div>
</section>

<section id="funciones">
  <h2><span style="background:var(--naranja)"></span>Funciones principales</h2>
  <p class="sub">Todo lo que hace la aplicación, resumido.</p>
  <div class="grid">
    <div class="card"><h3><i style="background:var(--morado)">🧭</i>Alta guiada del contrato</h3><p>El asistente <strong>Configurar paso a paso</strong> te pide los datos del contrato en seis pasos, con explicaciones y comprobaciones en cada uno. Solo hacen falta cuatro datos para empezar: inicio, plazo, kilómetros al año y cuota.</p></div>
    <div class="card"><h3><i style="background:var(--azul)">📍</i>Kilómetros reales</h3><p>Anota lecturas del cuentakilómetros con fecha y foto opcional. La app calcula la desviación frente a los kilómetros teóricos del contrato.</p></div>
    <div class="card"><h3><i style="background:var(--teal)">📈</i>Proyección</h3><p>Estimación de kilómetros a fin de contrato según el ritmo acumulado, el de los últimos 6 meses o un ritmo manual, con gráfico de evolución.</p></div>
    <div class="card"><h3><i style="background:var(--naranja)">💶</i>Liquidación estimada</h3><p>Abono o cargo previsto por kilómetros no recorridos o de exceso, aplicando los umbrales y recargos de tu contrato, y coste total con IVA.</p></div>
    <div class="card"><h3><i style="background:var(--rosa)">🛣️</i>¿Cuánto puedo conducir?</h3><p>Margen de kilómetros disponibles hasta el fin del contrato y por día para no pagar exceso.</p></div>
    <div class="card"><h3><i style="background:var(--morado)">⛽</i>Repostajes y consumo</h3><p>Registro de repostajes con litros y precio por litro (manual o precio medio de mercado del combustible de tu vehículo en tu provincia), consumo en l/100 km.</p></div>
    <div class="card"><h3><i style="background:#D81B60">🧾</i>Foto del tique</h3><p>Haz una foto al tique del repostaje: la app lee en el propio móvil la fecha, el importe, el precio por litro y los litros, y rellena el formulario para que solo tengas que revisarlo.</p></div>
    <div class="card"><h3><i style="background:#0097A7">🧾</i>Otros gastos</h3><p>Peajes, aparcamiento, lavados, neumáticos… se suman al coste real por kilómetro.</p></div>
    <div class="card"><h3><i style="background:#43A047">📄</i>Informe PDF y Excel</h3><p>Informe de estado de una página en PDF para compartir, y exportación a Excel con fórmulas vivas.</p></div>
    <div class="card"><h3><i style="background:#F4511E">🚔</i>Consulta de multas</h3><p>Busca la matrícula en el Tablón Edictal Único del BOE, con revisión semanal automática y aviso si aparece algo.</p></div>
    <div class="card"><h3><i style="background:#3949AB">🔔</i>Recordatorios y widget</h3><p>Aviso mensual para anotar los kilómetros, aviso del ajuste anual y widget en la pantalla de inicio con el estado actual.</p></div>
    <div class="card"><h3><i style="background:#8E24AA">🌙</i>Modo oscuro</h3><p>Tema claro, oscuro o según el sistema, con una interfaz colorida y legible.</p></div>
    <div class="card"><h3><i style="background:#00897B">💾</i>Copias de seguridad</h3><p>Guarda y restaura una copia completa (contrato, mediciones, repostajes, gastos y fotos) en un único fichero ZIP.</p></div>
    <div class="card"><h3><i style="background:#6D4C41">📞</i>Datos de contacto de la empresa de renting</h3><p>Teléfonos y correo de tu compañía de renting a un toque, configurables en Ajustes.</p></div>
  </div>
</section>

<section id="instalar">
  <h2><span style="background:var(--rosa)"></span>Cómo instalar la APK</h2>
  <p class="sub">La aplicación no está en Google Play, por lo que se instala directamente desde el fichero.</p>
  <ol class="pasos">
    <li>Desde el navegador de tu móvil o tablet Android, pulsa <strong>Descargar APK</strong> y guarda el fichero <code>SeguimientoRenting.apk</code>.</li>
    <li>Abre el fichero descargado (desde la notificación de descarga o con la app <em>Archivos</em>).</li>
    <li>Si Android lo pide, permite <strong>instalar aplicaciones de esta fuente</strong> (navegador o gestor de archivos). Es una pregunta habitual para cualquier app fuera de Google Play.</li>
    <li>Pulsa <strong>Instalar</strong>. Al abrir la app por primera vez, ve a <strong>Ajustes</strong> y rellena los datos de tu contrato; hasta entonces no se pueden añadir mediciones.</li>
  </ol>
  <div class="nota" style="margin-top:16px">Si Play Protect muestra un aviso, elige <em>Instalar de todos modos</em>. La app no requiere cuenta ni contiene anuncios.</div>
  <div class="nota" style="margin-top:12px">Seguimiento Renting es una aplicación independiente de uso privado y <strong>no representa a ninguna entidad pública</strong> (como el BOE o la DGT). Solo usa Internet para consultar datos públicos abiertos de combustible (<a href="https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/help" target="_blank" rel="noopener">servicio oficial de precios de carburantes</a>) y notificaciones públicas en el <a href="https://www.boe.es/notificaciones/" target="_blank" rel="noopener">Tablón Edictal Único del BOE</a>.</div>
  <div class="req">
    <span>Android 8.0+</span><span>Versión {VERSION}</span><span>Sin anuncios</span><span>Datos solo en tu dispositivo</span>
  </div>
</section>

<section id="descargas">
  <h2><span style="background:var(--morado)"></span>Descargas</h2>
  <p class="sub">Ambos ficheros se descargan desde esta misma página.</p>
  <div class="dl">
    <div class="card">
      <h3><i style="background:var(--amarillo);color:#1F2937">📱</i>Aplicación Android</h3>
      <p>Fichero de instalación <code>SeguimientoRenting.apk</code> · versión {VERSION} · {apk:.0f} MB</p>
      <a class="btn apk" href="{APK_HREF}" download>Descargar APK</a>
    </div>
    <div class="card">
      <h3><i style="background:var(--azul)">📘</i>Manual de usuario</h3>
      <p>Guía completa con capturas de todas las pantallas · PDF · {pdf:.1f} MB</p>
      <a class="btn pdf" href="{PDF_HREF}" download>Descargar manual (PDF)</a>
    </div>
  </div>
</section>

</div>
</main>

<footer>
  <div class="wrap">Seguimiento Renting · versión {VERSION} · Página actualizada el {fecha}.<br>Las capturas mostradas utilizan datos ficticios de ejemplo.<br>Esta aplicación no representa a ninguna entidad pública. Fuentes oficiales: <a href="https://www.boe.es/notificaciones/" target="_blank" rel="noopener">BOE (TEU)</a> · <a href="https://sede.dgt.gob.es/es/multas/" target="_blank" rel="noopener">Sede DGT</a> · <a href="https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/help" target="_blank" rel="noopener">Precios de carburantes (Ministerio)</a>{enlaces}</div>
</footer>

</body>
</html>
"""
open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "Web", "index.html") if LOCAL else os.path.join(os.path.dirname(os.path.abspath(__file__)), "docs", "index.html"), "w").write(html)
print("index.html", len(html)//1024, "KB")
