# -*- coding: utf-8 -*-
"""Manual de usuario de Seguimiento Renting (PDF con reportlab)."""
import os, datetime
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_JUSTIFY
from reportlab.platypus import (BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Image, Table, TableStyle,
                                PageBreak, KeepTogether, NextPageTemplate, ListFlowable, ListItem)
from reportlab.platypus.tableofcontents import TableOfContents
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from PIL import Image as PILImage

S = os.path.dirname(os.path.abspath(__file__))
M = os.path.join(S, "manual")
OUT = "/Users/manursan/Documents/PERSONALES/RENTING_PEUGEOT/Manual_Seguimiento_Renting.pdf"

F = "/System/Library/Fonts/Supplemental/"
pdfmetrics.registerFont(TTFont("Arial", F + "Arial.ttf"))
pdfmetrics.registerFont(TTFont("Arial-Bold", F + "Arial Bold.ttf"))
pdfmetrics.registerFont(TTFont("Arial-Italic", F + "Arial Italic.ttf"))
pdfmetrics.registerFont(TTFont("Arial-BoldItalic", F + "Arial Bold Italic.ttf"))
pdfmetrics.registerFontFamily("Arial", normal="Arial", bold="Arial-Bold", italic="Arial-Italic", boldItalic="Arial-BoldItalic")
pdfmetrics.registerFont(TTFont("ArialUnicode", F + "Arial Unicode.ttf"))
# Símbolos que Arial no tiene (p. ej. los tres puntos verticales del menú) se pintan con Arial Unicode
MENU = '<img src="%s" width="5.5" height="13.75" valign="-3"/>' % os.path.join(M, "menu_dots.png")
def fix(t): return t.replace("\u22ee", MENU)

AZUL = colors.HexColor("#2F6BFF"); TEAL = colors.HexColor("#00B8A9"); VERDE = colors.HexColor("#1E8E4E")
NARANJA = colors.HexColor("#FF7A1A"); MORADO = colors.HexColor("#8B5CF6"); ROJO = colors.HexColor("#E5484D")
TINTA = colors.HexColor("#1B1F2B"); GRIS = colors.HexColor("#555C6E"); SUAVE = colors.HexColor("#EEF2FA"); LINEA = colors.HexColor("#D5DAE6")

st = {
    "title": ParagraphStyle("title", fontName="Arial-Bold", fontSize=30, leading=36, textColor=colors.white),
    "subtitle": ParagraphStyle("subtitle", fontName="Arial", fontSize=14, leading=18, textColor=colors.white),
    "h1": ParagraphStyle("h1", fontName="Arial-Bold", fontSize=20, leading=24, textColor=AZUL, spaceBefore=6, spaceAfter=10),
    "h2": ParagraphStyle("h2", fontName="Arial-Bold", fontSize=13.5, leading=17, textColor=TINTA, spaceBefore=12, spaceAfter=5, keepWithNext=1),
    "h3": ParagraphStyle("h3", fontName="Arial-Bold", fontSize=11, leading=14, textColor=GRIS, spaceBefore=8, spaceAfter=3, keepWithNext=1),
    "body": ParagraphStyle("body", fontName="Arial", fontSize=9.8, leading=13.5, textColor=TINTA, alignment=TA_JUSTIFY, spaceAfter=5),
    "bullet": ParagraphStyle("bullet", fontName="Arial", fontSize=9.8, leading=13.5, textColor=TINTA, leftIndent=0, spaceAfter=2),
    "cap": ParagraphStyle("cap", fontName="Arial-Italic", fontSize=8, leading=10, textColor=GRIS, alignment=TA_CENTER),
    "note": ParagraphStyle("note", fontName="Arial", fontSize=9.2, leading=12.5, textColor=TINTA),
    "toc1": ParagraphStyle("toc1", fontName="Arial-Bold", fontSize=10.5, leading=15, textColor=TINTA),
    "toc2": ParagraphStyle("toc2", fontName="Arial", fontSize=9.5, leading=13, textColor=GRIS, leftIndent=14),
    "small": ParagraphStyle("small", fontName="Arial", fontSize=8.2, leading=11, textColor=GRIS),
    "cell": ParagraphStyle("cell", fontName="Arial", fontSize=8.8, leading=11.5, textColor=TINTA),
    "cellb": ParagraphStyle("cellb", fontName="Arial-Bold", fontSize=8.8, leading=11.5, textColor=TINTA),
}

def P(t, s="body"): return Paragraph(fix(t), st[s])

def bullets(items):
    return ListFlowable([ListItem(Paragraph(fix(i), st["bullet"]), leftIndent=12, value="•") for i in items],
                        bulletType="bullet", start="•", leftIndent=12, bulletFontName="Arial", bulletFontSize=9)

def shot(name, w=5.6*cm, crop=None):
    """Captura de pantalla a la anchura indicada (manteniendo proporción). crop=(y0,y1) en px para recortar."""
    path = os.path.join(M, name + ".png")
    im = PILImage.open(path).convert("RGB")
    if crop:
        im = im.crop((0, crop[0], im.width, crop[1]))
    if im.width > 640:
        im = im.resize((640, int(im.height * 640 / im.width)), PILImage.LANCZOS)
    path = os.path.join(M, "_emb_" + name + ("_%d_%d" % crop if crop else "") + ".jpg"); im.save(path, quality=88)
    ratio = im.height / im.width
    return Image(path, width=w, height=w * ratio)

def figrow(items, w=5.6*cm):
    """Fila de capturas con pie: items = [(nombre, pie, crop?), ...]"""
    imgs, caps = [], []
    for it in items:
        name, cap = it[0], it[1]; crop = it[2] if len(it) > 2 else None
        imgs.append(shot(name, w, crop)); caps.append(Paragraph(fix(cap), st["cap"]))
    t = Table([imgs, caps], colWidths=[w + 0.5*cm] * len(items), hAlign="CENTER")
    t.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER"), ("VALIGN", (0, 0), (-1, 0), "TOP"),
                           ("TOPPADDING", (0, 1), (-1, 1), 3), ("BOTTOMPADDING", (0, 0), (-1, -1), 6)]))
    return KeepTogether([Spacer(1, 4), t])

def note(text, color=AZUL, title="Nota"):
    t = Table([[Paragraph(fix("<b>%s</b>  %s" % (title, text)), st["note"])]], colWidths=[16.4*cm])
    t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), SUAVE), ("LINEBEFORE", (0, 0), (0, -1), 3, color),
                           ("LEFTPADDING", (0, 0), (-1, -1), 10), ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                           ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6)]))
    return KeepTogether([Spacer(1, 3), t, Spacer(1, 6)])

def table(rows, widths, header=True):
    data = [[Paragraph(fix(c), st["cellb" if (header and i == 0) else "cell"]) for c in r] for i, r in enumerate(rows)]
    t = Table(data, colWidths=widths, hAlign="LEFT", repeatRows=1 if header else 0)
    style = [("GRID", (0, 0), (-1, -1), 0.4, LINEA), ("VALIGN", (0, 0), (-1, -1), "TOP"),
             ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4)]
    if header: style += [("BACKGROUND", (0, 0), (-1, 0), SUAVE)]
    t.setStyle(TableStyle(style))
    return t

# ---------- Plantilla con cabecera/pie y TOC ----------
class Doc(BaseDocTemplate):
    def __init__(self, fn, **kw):
        super().__init__(fn, pagesize=A4, leftMargin=2.3*cm, rightMargin=2.3*cm, topMargin=2.2*cm, bottomMargin=2*cm,
                         title="Seguimiento Renting · Manual de usuario", author="Seguimiento Renting", **kw)
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="f")
        self.addPageTemplates([PageTemplate(id="cover", frames=[frame], onPage=self.cover),
                               PageTemplate(id="normal", frames=[frame], onPage=self.decorate)])
    def cover(self, canv, doc):
        canv.saveState()
        canv.setFillColor(AZUL); canv.rect(0, A4[1] - 11*cm, A4[0], 11*cm, fill=1, stroke=0)
        canv.setFillColor(TEAL); canv.rect(0, A4[1] - 11.4*cm, A4[0], 0.4*cm, fill=1, stroke=0)
        # velocímetro estilizado
        cx, cy = A4[0] - 3.2*cm, A4[1] - 2.9*cm
        canv.setStrokeColor(colors.white); canv.setLineWidth(3.5); canv.arc(cx - 1.0*cm, cy - 1.0*cm, cx + 1.0*cm, cy + 1.0*cm, 200, 140)
        canv.setStrokeColor(colors.HexColor("#FFC857")); canv.setLineWidth(2.5); canv.line(cx, cy, cx + 0.6*cm, cy + 0.55*cm)
        canv.setFillColor(colors.HexColor("#FFC857")); canv.circle(cx, cy, 0.14*cm, fill=1, stroke=0)
        canv.restoreState()
    def decorate(self, canv, doc):
        canv.saveState()
        canv.setStrokeColor(LINEA); canv.setLineWidth(0.5)
        canv.line(doc.leftMargin, A4[1] - 1.5*cm, A4[0] - doc.rightMargin, A4[1] - 1.5*cm)
        canv.setFont("Arial", 8); canv.setFillColor(GRIS)
        canv.drawString(doc.leftMargin, A4[1] - 1.35*cm, "Seguimiento Renting · Manual de usuario")
        canv.drawRightString(A4[0] - doc.rightMargin, A4[1] - 1.35*cm, "Versión 1.0 · %s" % datetime.date.today().strftime("%d/%m/%Y"))
        canv.drawCentredString(A4[0] / 2, 1.2*cm, str(doc.page))
        canv.restoreState()
    def afterFlowable(self, fl):
        if isinstance(fl, Paragraph):
            if fl.style.name == "h1":
                key = "h1-%s" % self.seq.nextf("h1"); self.canv.bookmarkPage(key)
                self.notify("TOCEntry", (0, fl.getPlainText(), self.page, key))
            elif fl.style.name == "h2":
                key = "h2-%s" % self.seq.nextf("h2"); self.canv.bookmarkPage(key)
                self.notify("TOCEntry", (1, fl.getPlainText(), self.page, key))

def H1(t): return [PageBreak(), P(t, "h1")]
def H2(t): return [P(t, "h2")]

story = []
# ---------- Portada ----------
story += [Spacer(1, 2.2*cm), P("Seguimiento Renting", "title"), Spacer(1, 0.3*cm),
          P("Manual de usuario", "subtitle"), Spacer(1, 0.2*cm),
          P("Aplicación Android para el seguimiento de un contrato de renting: kilómetros, combustible, gastos, proyección y liquidación", "subtitle"),
          NextPageTemplate("normal"), Spacer(1, 4.6*cm)]
cover_tbl = Table([[shot("01_resumen_1", 4.4*cm), shot("30_proy_1", 4.4*cm), shot("20_rep_lista", 4.4*cm)]], colWidths=[5.2*cm]*3, hAlign="CENTER")
cover_tbl.setStyle(TableStyle([("ALIGN", (0, 0), (-1, -1), "CENTER")]))
story += [cover_tbl, Spacer(1, 0.8*cm),
          Paragraph("Versión 1.0 · %s · Todas las capturas de este manual usan datos ficticios de ejemplo." % datetime.date.today().strftime("%B %Y").capitalize(), st["cap"])]

# ---------- Índice ----------
toc = TableOfContents(); toc.levelStyles = [st["toc1"], st["toc2"]]; toc.dotsMinLevel = 0
st["h1toc"] = ParagraphStyle("h1toc", parent=st["h1"])
story += [PageBreak(), P("Índice", "h1toc"), toc]

# ---------- 1. Introducción ----------
story += H1("1. Introducción")
story += [P("<b>Seguimiento Renting</b> es una aplicación para Android pensada para quien tiene un coche en renting con un límite de kilómetros. "
            "Su objetivo es responder en todo momento a tres preguntas: <b>¿voy por encima o por debajo del kilometraje contratado?</b>, "
            "<b>¿cuánto me está costando realmente cada kilómetro?</b> y <b>¿cómo acabará el contrato: con abono, sin ajuste o con cargo por exceso?</b>"),
          P("La aplicación replica los cálculos de una hoja de seguimiento de kilómetros (kilómetros teóricos, desviación, coste por kilómetro, "
            "proyección a fin de contrato, liquidación y coste total con IVA) y añade lo que una hoja no puede ofrecer: registro desde el móvil, "
            "fotos del cuentakilómetros, precios de combustible de mercado, gráfica, informe en PDF, recordatorios, widget y consulta de sanciones publicadas en el BOE."),
          P("Todos los datos se guardan <b>únicamente en el dispositivo</b>. La aplicación no tiene cuentas de usuario ni envía información a ningún servidor propio; "
            "solo consulta dos fuentes públicas cuando se lo pides: los precios de carburantes del Ministerio de Industria y el Tablón Edictal Único del BOE.")]
story += H2("1.1 Requisitos")
story += [bullets(["Teléfono o tablet con <b>Android 8.0</b> o superior.",
                   "Conexión a Internet solo para el precio de mercado del combustible y la consulta de multas; el resto funciona sin conexión.",
                   "Unos 2 MB de espacio (más las fotos que añadas)."])]
story += H2("1.2 Cómo está organizado este manual")
story += [P("Los capítulos 2 y 3 explican cómo instalar la aplicación y los conceptos en los que se basa. Los capítulos 4 a 9 recorren cada pantalla. "
            "Los capítulos 10 y 11 describen el menú, el widget y las notificaciones. Al final encontrarás preguntas frecuentes, notas de privacidad y un apéndice con las fórmulas.")]

# ---------- 2. Instalación ----------
story += H1("2. Instalación y primer arranque")
story += H2("2.1 Instalar el APK")
story += [P("La aplicación se distribuye como un fichero <b>SeguimientoRenting.apk</b>. Para instalarlo:"),
          bullets(["Copia el fichero al dispositivo (cable USB, correo, Drive, descarga desde un navegador…).",
                   "Ábrelo desde la aplicación <i>Archivos</i> o desde la notificación de descarga.",
                   "Android pedirá permiso para <i>instalar aplicaciones desconocidas</i> a la aplicación desde la que lo abres. Concédelo una vez.",
                   "Pulsa <b>Instalar</b>. Si ya tenías una versión anterior, se actualiza encima y <b>conserva todos los datos</b>."]),
          note("En dispositivos Xiaomi (HyperOS/MIUI) puede aparecer además un permiso propio de <i>acceso a la red</i> para la aplicación. Si se deniega, "
               "las consultas de precio de mercado y de multas fallarán hasta que se conceda en Ajustes → Aplicaciones → Seguimiento Renting.", NARANJA, "Aviso")]
story += H2("2.2 Primer arranque")
story += [P("La primera vez la aplicación está <b>vacía</b>: no hay contrato ni mediciones. Las pestañas muestran el aviso <i>Contrato sin configurar</i> y no "
            "es posible anotar datos hasta que el contrato tenga fechas válidas, plazo y kilómetros anuales. Tienes dos formas de empezar:"),
          bullets(["<b>Introducir el contrato</b> en <i>Ajustes → Contrato</i>: fechas, plazo, km/año, cuotas y tarifas (capítulo 9.6).",
                   "<b>Restaurar una copia de seguridad</b> (menú ⋮ → <i>Restaurar copia de seguridad</i>) si vienes de otro dispositivo o de una instalación anterior (capítulo 10.4)."]),
          figrow([("01_resumen_1", "Resumen con el contrato configurado"), ("10_km_lista", "Kilómetros con mediciones"), ("40_ajustes_1", "Ajustes")])]

# ---------- 3. Conceptos ----------
story += H1("3. Conceptos clave")
story += [P("Conviene entender cinco ideas que aparecen en toda la aplicación.")]
story += H2("3.1 Kilómetros contratados y kilómetros teóricos")
story += [P("Los <b>km contratados</b> son los kilómetros anuales del contrato multiplicados por su duración en años (por ejemplo, 15.000 km/año × 3 años = 45.000 km). "
            "Los <b>km teóricos</b> en una fecha son los que deberías llevar si repartieras los km contratados de forma uniforme entre todos los días del contrato: "
            "el primer día son 0 y el último día coinciden exactamente con los contratados. La <b>desviación</b> es la diferencia entre tus km reales y los teóricos: "
            "negativa si vas por debajo del ritmo (bien), positiva si vas por encima.")]
story += H2("3.2 Ritmo y proyección")
story += [P("El <b>ritmo</b> son los km/día. La aplicación calcula el <b>real acumulado</b> (km de la última medida entre los días transcurridos) y el de los "
            "<b>últimos 6 meses</b>. Con uno de ellos (o con un valor manual) <b>proyecta</b> los km que tendrá el coche el día que termine el contrato. "
            "Ese valor, los <i>km estimados a fin de contrato</i>, es el que decide la liquidación.")]
story += H2("3.3 Liquidación: abono, banda neutra y cargo")
story += [P("Los contratos de renting suelen liquidar los kilómetros en tres tramos. Con los valores de ejemplo (45.000 km contratados, umbral de abono del 90 %):"),
          table([["Km al final del contrato", "Qué ocurre"],
                 ["Menos de 40.500 (90 %)", "La compañía te <b>abona</b> los km no recorridos por debajo del umbral, a una tarifa por km."],
                 ["Entre 40.500 y 45.000", "<b>Sin abono ni cargo</b>: la banda neutra."],
                 ["Más de 45.000", "Pagas cada km de <b>exceso</b> a una tarifa; si el exceso supera un porcentaje (umbral de recargo), la tarifa se multiplica por un recargo."]],
                [5.2*cm, 11.2*cm])]
story += H2("3.4 Coste por kilómetro")
story += [P("Suma la cuota del renting prorrateada hasta la fecha, el combustible y los otros gastos anotados, y lo divide entre los km recorridos. "
            "<b>Baja con el uso</b>: la cuota es fija y se reparte entre más kilómetros. La aplicación lo muestra desglosado en renting, combustible y otros.")]
story += H2("3.5 Consumo y precio de mercado")
story += [P("Para calcular el consumo (l/100 km) hacen falta litros. Si anotas los litros del tique, se usan directamente. Si no, la aplicación los deduce "
            "dividiendo el importe entre el <b>precio por litro</b>, que puedes escribir a mano o tomar del <b>precio medio de mercado</b> de la Gasolina 95 "
            "en esa fecha y provincia (datos abiertos del Ministerio de Industria, con histórico diario).")]

# ---------- 4. Navegación ----------
story += H1("4. Navegación general")
story += [P("La barra inferior tiene cinco pestañas, cada una con su color: <font color='#2F6BFF'><b>Resumen</b></font> (azul), "
            "<font color='#34B36B'><b>Kilómetros</b></font> (verde), <font color='#FF7A1A'><b>Repostajes</b></font> (naranja), "
            "<font color='#8B5CF6'><b>Proyección</b></font> (morado) y <font color='#00B8A9'><b>Ajustes</b></font> (turquesa). "
            "La barra superior toma el color de la pestaña activa. El menú <b>⋮</b> de la esquina superior derecha está disponible en todas las pantallas (capítulo 10)."),
          P("En Kilómetros y Repostajes, el botón redondo <b>+</b> añade un elemento; tocar una tarjeta lo abre para editarlo o eliminarlo. "
            "La aplicación respeta el tamaño de fuente del sistema y tiene modo claro y oscuro (Ajustes → Apariencia)."),
          figrow([("50_menu", "Menú ⋮"), ("60_oscuro", "Modo oscuro"), ("12_km_editar", "Editar un elemento (Eliminar / Cancelar / Guardar)")])]

# ---------- 5. Resumen ----------
story += H1("5. Pantalla Resumen")
story += [P("Es el cuadro de mando. Se recalcula al instante con cada dato que anotas. De arriba abajo:")]
story += H2("5.1 Cabecera")
story += [P("Km de la última medida con su fecha, días y meses que quedan de contrato, y dos barras: el porcentaje de <b>kilómetros</b> consumidos sobre los contratados "
            "y el porcentaje de <b>tiempo</b> transcurrido. Si la barra de kilómetros va por delante de la de tiempo, estás gastando km más deprisa de lo previsto.")]
story += H2("5.2 Ritmo")
story += [P("Km teóricos a la fecha de la última medida, desviación en km y en porcentaje (verde si vas por debajo, rojo si por encima), km/día real y del contrato, y el consumo si hay datos.")]
story += H2("5.3 ¿Cuánto puedo conducir?")
story += [P("La tarjeta más práctica: los km que te quedan hasta los contratados y el <b>máximo de km/día y km/mes</b> que puedes hacer desde hoy hasta el fin del contrato "
            "sin pagar exceso; y el ritmo para quedar por debajo del umbral de abono. Compara tu ritmo actual con ese máximo y te dice si vas dentro del margen."),
          figrow([("01_resumen_1", "Cabecera y Ritmo"), ("02_resumen_2", "Margen, Combustible y Coste por km"), ("03_resumen_3", "Liquidación y ajuste anual")])]
story += H2("5.4 Combustible")
story += [P("Total repostado, número de repostajes, combustible por km, coste diario y, si hay litros o precios, el <b>consumo</b> en l/100 km, el precio medio pagado y los litros totales. "
            "Si algún repostaje no tiene litros ni precio, el consumo se marca como estimado.")]
story += H2("5.5 Coste por kilómetro")
story += [P("Desglose <b>renting + combustible + otros gastos</b> por km, a la fecha de la última medida. Si el escenario de proyección hace que el coste a fin de contrato sea distinto "
            "(ritmo manual o cargo por exceso), aparece una línea adicional con ese valor.")]
story += H2("5.6 Liquidación fin de contrato")
story += [P("Km estimados a fin de contrato con el ritmo del escenario elegido, umbrales de abono y cargo, y el resultado: <i>abono a tu favor</i>, <i>sin abono ni cargo</i> o <i>cargo por exceso</i>, "
            "con el importe (sin IVA) y una explicación del tramo aplicado.")]
story += H2("5.7 Próximo ajuste anual")
story += [P("Muchos contratos hacen un ajuste a cuenta en cada aniversario. La tarjeta muestra la fecha del siguiente, los km previstos frente a los teóricos y si la desviación "
            "cae dentro de la banda de tolerancia (por defecto ±10 %). Lo que se pague o abone en estos ajustes se descuenta de la liquidación final.")]
story += H2("5.8 Otros gastos y coste de uso")
story += [P("Totales por categoría (peajes, parking, lavado…) y el <b>coste de uso hasta hoy</b>: cuotas devengadas + combustible + otros gastos.")]
story += H2("5.9 Coste total del contrato, IVA y contrato")
story += [P("Proyección del coste completo: cuotas, cuota irregular inicial (prorrateo de los días entre la entrega y el primer día 1), combustible proyectado y abono o cargo. "
            "Debajo, el desglose del IVA soportado, el IVA deducible según el porcentaje configurado y el coste neto. La última tarjeta resume los datos del contrato."),
          figrow([("04_resumen_4", "Otros gastos, coste total e IVA"), ("05_resumen_5", "IVA y datos del contrato")])]

# ---------- 6. Kilómetros ----------
story += H1("6. Pestaña Kilómetros")
story += [P("Lista de mediciones del cuentakilómetros, de la más reciente a la más antigua. Cada tarjeta muestra la fecha, el día de contrato, los km, la desviación frente a los teóricos "
            "(etiqueta verde o roja), el ritmo en km/día, los km teóricos, la gasolina acumulada hasta esa fecha y el coste/km a esa fecha. Si la medición tiene foto, aparece una miniatura; tócala para verla a pantalla completa.")]
story += H2("6.1 Añadir una medición")
story += [bullets(["Pulsa <b>+</b>. La fecha propuesta es hoy; tócala para cambiarla.",
                   "Escribe los <b>km del cuentakilómetros</b> (el valor total que marca el coche, no los km del viaje). Si son menores que una medida anterior, la aplicación avisa pero permite guardar.",
                   "Opcionalmente añade una nota y una <b>foto</b> del cuentakilómetros con la cámara o desde la galería; se guarda reducida (1.600 px) para no ocupar espacio.",
                   "Pulsa <b>Guardar</b>."]),
          P("Lo natural es anotar una medición cada mes (puedes activar un recordatorio en Ajustes) y siempre que quieras un cálculo exacto, por ejemplo antes y después de un viaje largo.")]
story += H2("6.2 Editar o eliminar")
story += [P("Toca la tarjeta. Cambia lo que necesites y pulsa <b>Guardar</b>, o pulsa <b>Eliminar</b> y confirma. Al eliminar una medición se borra también su foto."),
          figrow([("10_km_lista", "Lista de mediciones"), ("11_km_nueva", "Nueva medición con cámara/galería"), ("12_km_editar", "Edición con opción de eliminar")])]

# ---------- 7. Repostajes ----------
story += H1("7. Pestaña Repostajes y otros gastos")
story += [P("Tiene dos secciones, seleccionables arriba: <b>Repostajes</b> y <b>Otros gastos</b>. El botón + añade en la sección activa.")]
story += H2("7.1 Repostajes")
story += [P("Cada tarjeta muestra fecha, importe, acumulado, litros, precio por litro (indicando si fue manual o de mercado) y coste diario. La cabecera resume el total repostado y el consumo.")]
story += H2("7.2 Añadir un repostaje")
story += [bullets(["<b>Fecha</b> e <b>importe</b> en euros. El importe es obligatorio salvo que indiques litros y precio, en cuyo caso se calcula.",
                   "<b>Precio por litro</b>, con dos modos: <b>Manual</b> (lo escribes del tique) o <b>Mercado</b> (la aplicación consulta el precio medio de la Gasolina 95 en esa fecha "
                   "y en la provincia configurada; se muestra en pantalla y se actualiza si cambias la fecha). Si no hay conexión, se guarda sin precio y podrás completarlo después.",
                   "<b>Litros</b> (opcional). Si los dejas en blanco y hay precio, se calculan como importe ÷ precio y se muestra el resultado.",
                   "Nota opcional y <b>Guardar</b>."]),
          note("Si tienes repostajes antiguos sin litros ni precio, usa <b>menú ⋮ → Completar precios de mercado</b>: la aplicación busca el precio medio de cada fecha y lo asigna a todos los que "
               "no lo tengan, con lo que el consumo pasa a calcularse con todos los repostajes.", VERDE, "Consejo"),
          figrow([("20_rep_lista", "Lista de repostajes"), ("21_rep_nuevo_mercado", "Nuevo repostaje con precio de mercado"), ("22_rep_nuevo_manual", "Precio manual")])]
story += H2("7.3 Otros gastos")
story += [P("Gastos del coche que no forman parte del contrato: <b>peaje, aparcamiento, lavado, multa, mantenimiento, AdBlue u otro</b>. Se anotan con fecha, categoría, importe y nota. "
            "Entran en el coste por kilómetro y en el coste de uso hasta hoy, pero no en el coste del contrato ni en la liquidación."),
          figrow([("23_gastos_lista", "Otros gastos"), ("24_gasto_nuevo", "Nuevo gasto con categoría")])]

# ---------- 8. Proyección ----------
story += H1("8. Pestaña Proyección")
story += H2("8.1 Escenario")
story += [P("Elige con qué ritmo se proyecta hasta el fin del contrato:"),
          table([["Escenario", "Qué usa", "Cuándo conviene"],
                 ["Media", "El ritmo real acumulado desde el inicio del contrato.", "Uso estable; es el valor por defecto."],
                 ["6 meses", "El ritmo de los últimos 180 días (interpolando entre mediciones).", "Si tu uso ha cambiado (más o menos km que al principio). Requiere 6 meses de datos."],
                 ["Manual", "Un valor de km/día que escribes y aplicas.", "Para simular: «¿qué pasa si hago 30 km/día a partir de ahora?»"]],
                [2.4*cm, 7*cm, 7*cm]),
          P("Debajo se muestran el ritmo aplicado, el teórico del contrato, el combustible estimado por km, los km estimados a fin de contrato y el abono o cargo resultante. "
            "El escenario elegido se usa en todas las pantallas y en el informe.")]
story += H2("8.2 Gráfica")
story += [P("Puntos verdes: tus mediciones. Recta gris: km teóricos. Línea morada discontinua: proyección desde la última medida. Líneas punteadas: umbrales de abono (turquesa) y de cargo (rojo). "
            "Si los puntos van por debajo de la recta, vas por debajo del ritmo contratado.")]
story += H2("8.3 Proyección mensual")
story += [P("Tabla con los km previstos el día 15 de cada mes hasta el fin del contrato, los teóricos, la desviación y la gasolina acumulada estimada."),
          figrow([("30_proy_1", "Escenario y resultado"), ("31_proy_2", "Gráfica"), ("32_proy_3", "Tabla mensual")])]

# ---------- 9. Ajustes ----------
story += H1("9. Pestaña Ajustes")
story += H2("9.1 Apariencia")
story += [P("<b>Sistema</b> (sigue el tema del dispositivo), <b>Claro</b> u <b>Oscuro</b>. El cambio es inmediato y se recuerda.")]
story += H2("9.2 Compañía de renting")
story += [P("Tarjeta de contacto con tu compañía de renting: botones de llamada directa a los teléfonos que hayas configurado y un botón de correo que abre un mensaje con el número de contrato en el asunto. "
            "El nombre de la compañía, los teléfonos y el correo se introducen en el bloque <i>Contrato</i> (apartado 9.6); mientras estén vacíos, la tarjeta lo indica. "
            "La primera vez Android pide permiso para realizar llamadas; si se deniega, se abre el marcador con el número ya escrito.")]
story += H2("9.3 Multas")
story += [P("Consulta el <b>Tablón Edictal Único del BOE</b>, donde la DGT y los ayuntamientos publican las sanciones de tráfico que no han podido notificar (últimos 3 meses)."),
          bullets(["<b>Matrícula a consultar</b>: viene rellena con la del contrato; puedes escribir cualquier otra (las consultas de otras matrículas no se guardan).",
                   "<b>Consultar ahora</b>: la aplicación hace la consulta en segundo plano y muestra el resultado en la tarjeta: <i>sin sanciones</i> o la lista de anuncios (fecha, organismo, texto) con enlace al PDF.",
                   "<b>Revisar cada semana y avisar</b>: consulta automática semanal de la matrícula del contrato con notificación si aparece un anuncio nuevo.",
                   "<b>Ver en el BOE</b> y <b>Sede DGT</b> abren las páginas oficiales en el navegador. Si una consulta falla, aparece <i>Probar conexión con el BOE</i>, que comprueba red, DNS y respuesta paso a paso."]),
          note("En un renting el titular del vehículo es la compañía: las multas se le notifican a ella, que identifica al conductor y se las reenvía. Al tablón del BOE solo llegan las que no se han podido notificar. "
               "Las sanciones que ya estén a tu nombre se consultan en la sede de la DGT con identificación (Cl@ve o certificado).", NARANJA, "Importante"),
          figrow([("40_ajustes_1", "Apariencia y compañía de renting"), ("47_ajustes_multas", "Consulta de multas con resultado"), ("42_ajustes_3", "Contrato")])]
story += H2("9.4 Recordatorios")
story += [P("<b>Anotar los kilómetros cada mes</b>: notificación el día del mes que elijas (1–28) a las 10:00, con la última medida y el margen diario. "
            "<b>Aviso previo al ajuste anual</b>: notificación 30 días antes de cada aniversario del contrato con la previsión de km. Ambos piden el permiso de notificaciones la primera vez y sobreviven al reinicio del dispositivo.")]
story += H2("9.5 Precio de mercado")
story += [P("Provincia que se usa para el precio medio de la Gasolina 95 (o <i>Toda España</i>). Elegir tu provincia hace la consulta más rápida y más representativa de lo que pagas.")]
story += H2("9.6 Contrato, cuotas, liquidación e IVA")
story += [P("Aquí se introducen los parámetros que alimentan todos los cálculos. Se guardan al pulsar <b>Guardar</b>; <b>Descartar cambios</b> vuelve a los valores guardados."),
          table([["Bloque", "Campos"],
                 ["Contrato", "Nº de contrato, vehículo, matrícula, compañía de renting con sus teléfonos y correo, inicio (puesta a disposición), fin, plazo en meses y km/año. Debajo se muestran los km contratados resultantes."],
                 ["Cuotas", "Cuota mensual con IVA, cuota sin IVA, parte de reparación de daños (informativa) y depósito en garantía (recuperable, no se cuenta como coste)."],
                 ["Liquidación de kilómetros", "€/km no recorrido (abono), €/km de exceso (cargo), umbral de abono (%), umbral de recargo (%) y recargo (×)."],
                 ["IVA", "Tipo de IVA y porcentaje de deducción aplicado (para autónomos y empresas; si no deduces IVA, pon 0)."]],
                [3.6*cm, 12.8*cm]),
          P("La aplicación <b>valida la coherencia</b> antes de guardar y lista los problemas en una tarjeta roja: fin posterior al inicio, plazo coherente con las fechas (±1 mes), "
            "km/año y cuota mayores que cero, cuota sin IVA no superior a la cuota con IVA y coherente con el tipo de IVA, reparación de daños no superior a la cuota sin IVA, porcentajes entre 0 y 100 y recargo ≥ 1."),
          figrow([("43_ajustes_4", "Contrato y liquidación"), ("44_ajustes_5", "Cuotas e IVA"), ("46_ajustes_7", "Borrar datos")])]
story += H2("9.7 Borrar datos")
story += [P("Deja la aplicación vacía: contrato, mediciones, repostajes, gastos y fotos (se conservan solo la apariencia y los recordatorios). Como protección, el botón permanece "
            "<b>deshabilitado hasta que exista una copia de seguridad de los datos actuales</b>: la tarjeta indica <i>Sin copia de los datos actuales</i> o <i>Copia al día</i>. "
            "Si después de la copia anotas algo, hará falta una copia nueva. La confirmación exige escribir <b>BORRAR</b>. Para recuperar los datos, restaura la copia desde el menú ⋮.")]

# ---------- 10. Menú ----------
story += H1("10. Menú ⋮")
story += H2("10.1 Informe PDF")
story += [P("Genera un <b>informe de estado de una página</b> (A4) y abre la hoja de compartir de Android para enviarlo por WhatsApp o correo, guardarlo en Drive o imprimirlo. Contiene: "
            "cabecera con contrato y fechas, cuatro indicadores (km, desviación, ritmo, coste/km), la gráfica, margen y ajuste anual, liquidación, combustible y gastos, coste del contrato con IVA y la tabla de las últimas mediciones."),
          KeepTogether([Table([[Image(os.path.join(M, "manual_informe.png"), width=10.5*cm, height=10.5*cm*1.414)]], colWidths=[16.4*cm], style=[("ALIGN", (0, 0), (-1, -1), "CENTER")]),
          Paragraph("Informe PDF generado por la aplicación (datos ficticios)", st["cap"])])]
story += H2("10.2 Exportar a Excel (.xlsx)")
story += [P("Crea un libro Excel con la misma disposición que la hoja de seguimiento original: tabla de kilómetros (fecha, reales, teóricos, desviación, km/día, gasolina, coste/km, desviación %), "
            "repostajes, parámetros del contrato y bloques de seguimiento, liquidación, coste total e IVA. Las celdas llevan <b>fórmulas</b>, no solo valores, de modo que puedes cambiar un parámetro en Excel y ver el efecto.")]
story += H2("10.3 Completar precios de mercado")
story += [P("Recorre los repostajes sin precio por litro y les asigna el precio medio de mercado de su fecha (según la provincia configurada). Muestra el progreso en pantalla y, al terminar, cuántos se han completado.")]
story += H2("10.4 Copia de seguridad y restauración")
story += [P("<b>Guardar copia de seguridad</b> crea un fichero ZIP con nombre <i>seguimiento_renting_AAAA-MM-DD_HHMM.zip</i> que contiene el contrato, todas las mediciones, repostajes, gastos y las fotos. "
            "Elige dónde guardarlo (Descargas, Drive…). <b>Restaurar copia de seguridad</b> abre el selector de archivos; al elegir el ZIP se sustituyen los datos actuales por los de la copia. "
            "Es la forma de pasar la aplicación a otro dispositivo o de recuperar los datos tras un borrado."),
          note("Haz una copia después de cada tanda de cambios importantes (por ejemplo tras <i>Completar precios de mercado</i>). La copia guarda el estado de ese momento; lo que anotes después no está en ella.", AZUL, "Recomendación")]

# ---------- 11. Widget y notificaciones ----------
story += H1("11. Widget y notificaciones")
story += H2("11.1 Widget de pantalla de inicio")
story += [P("Mantén pulsado un hueco de la pantalla de inicio → <i>Widgets</i> → <b>Seguimiento Renting</b>. Muestra los km actuales con su fecha, la desviación frente al contrato y el máximo "
            "de km/día y km/mes sin cargo. Se actualiza cada vez que guardas datos y al tocarlo abre la aplicación."),
          KeepTogether([Table([[Image(os.path.join(M, "manual_widget.png"), width=9*cm, height=9*cm*440/1000)]], colWidths=[16.4*cm], style=[("ALIGN", (0, 0), (-1, -1), "CENTER")]),
          Paragraph("Widget", st["cap"])])]
story += H2("11.2 Notificaciones")
story += [P("Tres tipos, todos opcionales y configurables en Ajustes: recordatorio mensual de anotar los km, aviso previo al ajuste anual y aviso de anuncio nuevo en el BOE. "
            "Al tocarlas se abre la aplicación."),
          figrow([("70_notificaciones", "Recordatorio mensual y aviso de ajuste anual", (0, 1400))], w=7*cm)]

# ---------- 12. FAQ ----------
story += H1("12. Preguntas frecuentes")
faq = [
    ("¿Qué km tengo que anotar, los del viaje o los del cuentakilómetros?", "Siempre el <b>total del cuentakilómetros</b>. La aplicación calcula las diferencias."),
    ("Anoté un repostaje pero el consumo no cambia.", "El consumo se calcula con los repostajes hasta la fecha de la <b>última medición de km</b>. Anota una medición posterior al repostaje."),
    ("¿Por qué el coste por km baja con el tiempo?", "Porque la cuota mensual es fija y se reparte entre más kilómetros. Es normal; se estabiliza con el uso."),
    ("El precio de mercado tarda mucho.", "Con <i>Toda España</i> se descargan unos 4 MB por consulta. Elige tu provincia en Ajustes → Precio de mercado (unos 300 KB)."),
    ("«No se pudo consultar» en Multas.", "Suele ser falta de red en ese momento (por ejemplo justo tras encender el dispositivo). Repite; si persiste, pulsa <i>Probar conexión con el BOE</i> y anota el mensaje."),
    ("«Sin sanciones» en el BOE, ¿significa que no tengo multas?", "Significa que no hay anuncios <i>no notificados</i> para esa matrícula en los últimos 3 meses. Las multas notificadas normalmente no pasan por el tablón."),
    ("El botón Borrar todos los datos está gris.", "Guarda antes una copia de seguridad desde el menú ⋮. Si anotaste algo después de la última copia, necesitas otra."),
    ("Restauré una copia y faltan datos recientes.", "La copia contiene el estado del momento en que se hizo. Restaura una copia más reciente o vuelve a anotar los datos."),
    ("¿Se pueden llevar dos contratos?", "No en la misma instalación. Al terminar un contrato: guarda una copia, borra todos los datos e introduce el nuevo; la copia conserva el anterior para consultarlo."),
    ("¿Dónde están mis datos? ¿Se suben a algún sitio?", "Solo en el dispositivo (y en la copia automática de Android si la tienes activada). Nada se envía a servidores propios; ver capítulo 13."),
    ("Actualicé la aplicación, ¿pierdo algo?", "No. Instalar un APK nuevo encima conserva todos los datos. Solo se pierden al desinstalar o al borrar los datos de la aplicación desde Android."),
]
story += [table([["Pregunta", "Respuesta"]] + [[q, a] for q, a in faq], [6*cm, 10.4*cm])]

# ---------- 13. Privacidad ----------
story += H1("13. Privacidad y fuentes de datos")
story += [bullets(["Los datos (contrato, mediciones, repostajes, gastos, fotos y ajustes) se guardan en el almacenamiento privado de la aplicación. No hay cuentas ni sincronización.",
                   "La <b>copia automática de Android</b> (Google Drive, si está activada en el dispositivo) incluye los datos y ajustes pero no las fotos; para las fotos usa la copia manual ZIP.",
                   "<b>Precio de mercado</b>: consulta los datos abiertos de precios de carburantes del Ministerio de Industria (Geoportal de gasolineras). Se envía únicamente la fecha, la provincia y el tipo de carburante.",
                   "<b>Multas</b>: consulta el buscador público del Tablón Edictal Único del BOE. Se envía únicamente la matrícula como texto de búsqueda.",
                   "<b>Llamadas</b>: el permiso de teléfono solo se usa al pulsar un botón de llamada. <b>Notificaciones</b>: solo para los recordatorios que actives.",
                   "Ambos servicios públicos pueden cambiar de formato; en ese caso la función afectada mostrará un error claro en lugar de un resultado incorrecto."])]

# ---------- Apéndice A ----------
story += H1("Apéndice A. Fórmulas")
story += [P("Notación: <i>d</i> = días desde el inicio del contrato hasta la fecha; <i>D</i> = días totales del contrato; <i>K</i> = km contratados; <i>km</i> = km reales en la fecha; <i>C</i> = cuota mensual con IVA."),
          table([["Magnitud", "Fórmula"],
                 ["Km contratados (K)", "km/año ÷ 12 × meses de plazo"],
                 ["Km/día teóricos", "K ÷ D"],
                 ["Km teóricos en una fecha", "d × K ÷ D"],
                 ["Desviación", "km − teóricos; en % = desviación ÷ teóricos"],
                 ["Km/día real acumulado", "km ÷ d"],
                 ["Km/día últimos 6 meses", "(km última medida − km hace 180 días, interpolado) ÷ 180"],
                 ["Gasto de gasolina a una fecha", "Suma de los repostajes con fecha ≤ fecha"],
                 ["Coste/km a una fecha", "(d × C × 12 ÷ 365 + gasolina + otros gastos) ÷ km"],
                 ["Km estimados a fin de contrato", "km última medida + ritmo del escenario × días hasta el fin"],
                 ["Combustible proyectado", "gasolina hasta la última medida + (€/km de combustible) × (km estimados − km última medida)"],
                 ["Umbral de abono", "K × umbral de liquidación (p. ej. 90 %)"],
                 ["Abono", "Si km estimados &lt; umbral de abono: (umbral − km estimados) × €/km no recorrido"],
                 ["Cargo", "Si km estimados &gt; K: (km estimados − K) × €/km exceso × (recargo si el exceso ÷ K &gt; umbral de recargo)"],
                 ["Cuota irregular inicial", "C × (días desde la entrega hasta el día 1 siguiente) ÷ (días del mes de entrega)"],
                 ["Coste total del contrato", "meses × C + cuota irregular + combustible proyectado − abono (o + cargo)"],
                 ["IVA soportado", "IVA de las cuotas + IVA de la cuota irregular + IVA incluido en el combustible"],
                 ["IVA deducible / coste neto", "IVA soportado × % deducción; coste neto = coste total − IVA deducible"],
                 ["Consumo (l/100 km)", "litros (anotados o importe ÷ precio/litro) hasta la última medida ÷ km × 100"],
                 ["Margen sin cargo", "(K − km última medida) ÷ días hasta el fin → km/día; × 30,44 → km/mes"]],
                [5.2*cm, 11.2*cm])]

# ---------- Apéndice B ----------
story += H1("Apéndice B. Ficha técnica")
story += [table([["Característica", "Detalle"],
                 ["Plataforma", "Android 8.0 (API 26) o superior; teléfonos y tablets"],
                 ["Tamaño", "Aproximadamente 1,6 MB"],
                 ["Permisos", "Internet (precios y BOE), notificaciones (recordatorios), teléfono (llamada directa), cámara (a través de la app de cámara del sistema; no requiere permiso propio)"],
                 ["Datos", "Fichero JSON en almacenamiento privado + carpeta de fotos; copia ZIP manual; copia automática de Android"],
                 ["Fuentes externas", "Precios de carburantes: sedeaplicaciones.minetur.gob.es · Multas: www.boe.es (Tablón Edictal Único)"],
                 ["Formatos de exportación", "PDF (informe de una página), XLSX (libro con fórmulas), ZIP (copia de seguridad)"],
                 ["Idioma", "Español"]],
                [4*cm, 12.4*cm])]

doc = Doc(OUT)
doc.multiBuild(story)
print("OK", OUT, os.path.getsize(OUT))
