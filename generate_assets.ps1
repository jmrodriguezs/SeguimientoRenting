Add-Type -AssemblyName System.Drawing

$baseDir = "c:\Users\Josema\Documents\SeguimientoRenting\seguimiento-renting"
$assetsDir = Join-Path $baseDir "play_store_assets"
$screenshotsDir = Join-Path $assetsDir "screenshots"

if (-not (Test-Path $screenshotsDir)) {
    New-Item -ItemType Directory -Force -Path $screenshotsDir | Out-Null
}

# -------------------------------------------------------------
# 1. ICON 512x512
# -------------------------------------------------------------
$iconPath = Join-Path $assetsDir "icon_512.png"
$bmpIcon = New-Object System.Drawing.Bitmap(512, 512, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmpIcon)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

# Background color #1B4B8A
$bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml('#1B4B8A'))
$g.FillRectangle($bgBrush, 0, 0, 512, 512)

$scale = 512.0 / 108.0

# White Arc (Speedometer frame)
$arcPen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, [float](6.0 * $scale))
$arcPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$arcPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$arcRect = New-Object System.Drawing.RectangleF([float](30.0 * $scale), [float](42.0 * $scale), [float](48.0 * $scale), [float](48.0 * $scale))
$g.DrawArc($arcPen, $arcRect, 180.0, 180.0)

# Bottom horizontal line
$basePen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, [float](4.0 * $scale))
$basePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$basePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$g.DrawLine($basePen, [float](34.0 * $scale), [float](74.0 * $scale), [float](74.0 * $scale), [float](74.0 * $scale))

# Gold Needle
$goldColor = [System.Drawing.ColorTranslator]::FromHtml('#FFC857')
$needlePen = New-Object System.Drawing.Pen($goldColor, [float](4.0 * $scale))
$needlePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$needlePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$g.DrawLine($needlePen, [float](54.0 * $scale), [float](66.0 * $scale), [float](66.0 * $scale), [float](46.0 * $scale))

# Center pin circle
$pinBrush = New-Object System.Drawing.SolidBrush($goldColor)
$pinRect = New-Object System.Drawing.RectangleF([float](50.0 * $scale), [float](62.0 * $scale), [float](8.0 * $scale), [float](8.0 * $scale))
$g.FillEllipse($pinBrush, $pinRect)

$g.Dispose()
$bmpIcon.Save($iconPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmpIcon.Dispose()
Write-Output "Generated: $iconPath"

# -------------------------------------------------------------
# 2. FEATURE GRAPHIC 1024x500
# -------------------------------------------------------------
$featPath = Join-Path $assetsDir "feature_graphic_1024x500.png"
$bmpFeat = New-Object System.Drawing.Bitmap(1024, 500, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gf = [System.Drawing.Graphics]::FromImage($bmpFeat)
$gf.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$gf.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

# Linear gradient background from #1B4B8A (left) to #0F2A52 (right)
$c1 = [System.Drawing.ColorTranslator]::FromHtml('#1B4B8A')
$c2 = [System.Drawing.ColorTranslator]::FromHtml('#0F2548')
$featRect = New-Object System.Drawing.Rectangle(0, 0, 1024, 500)
$gradBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($featRect, $c1, $c2, 35.0)
$gf.FillRectangle($gradBrush, $featRect)

# Draw decorative accent arc on background
$accentPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(25, 255, 255, 255), 40.0)
$gf.DrawEllipse($accentPen, 550, -100, 600, 600)
$accentPen.Dispose()

# Draw Speedometer Emblem on Left
$embX = 140.0
$embY = 250.0
$s = 2.4

$fArcPen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, [float](6.0 * $s))
$fArcPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$fArcPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$fArcRect = New-Object System.Drawing.RectangleF([float]($embX - 24.0 * $s), [float]($embY - 24.0 * $s), [float](48.0 * $s), [float](48.0 * $s))
$gf.DrawArc($fArcPen, $fArcRect, 180.0, 180.0)

$fBasePen = New-Object System.Drawing.Pen([System.Drawing.Color]::White, [float](4.0 * $s))
$fBasePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$fBasePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$gf.DrawLine($fBasePen, [float]($embX - 20.0 * $s), [float]($embY + 8.0 * $s), [float]($embX + 20.0 * $s), [float]($embY + 8.0 * $s))

$fNeedlePen = New-Object System.Drawing.Pen($goldColor, [float](4.0 * $s))
$fNeedlePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$fNeedlePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$gf.DrawLine($fNeedlePen, [float]$embX, [float]$embY, [float]($embX + 12.0 * $s), [float]($embY - 20.0 * $s))

$fPinBrush = New-Object System.Drawing.SolidBrush($goldColor)
$gf.FillEllipse($fPinBrush, [float]($embX - 4.0 * $s), [float]($embY - 4.0 * $s), [float](8.0 * $s), [float](8.0 * $s))

# Text
$fontTitle = New-Object System.Drawing.Font("Segoe UI", 42, [System.Drawing.FontStyle]::Bold)
$fontSub = New-Object System.Drawing.Font("Segoe UI", 18, [System.Drawing.FontStyle]::Regular)
$fontBadge = New-Object System.Drawing.Font("Segoe UI", 13, [System.Drawing.FontStyle]::Bold)

$whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
$subBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(210, 225, 250))
$goldBrush = New-Object System.Drawing.SolidBrush($goldColor)

$textLeft = 280.0
$gf.DrawString("Seguimiento Renting", $fontTitle, $whiteBrush, $textLeft, 150.0)
$gf.DrawString("Control de kilometraje, repostajes, proyección y gastos", $fontSub, $subBrush, $textLeft, 230.0)
$gf.DrawString("• 100% Privado y en el dispositivo  • Sin anuncios  • Exporta a Excel y PDF", $fontBadge, $goldBrush, $textLeft, 280.0)

$gf.Dispose()
$bmpFeat.Save($featPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmpFeat.Dispose()
Write-Output "Generated: $featPath"

# -------------------------------------------------------------
# 3. SCREENSHOTS
# -------------------------------------------------------------
$shotsToCopy = @(
    @{ src = "01_resumen_1.png"; dst = "01_resumen_principal.png" },
    @{ src = "02_resumen_2.png"; dst = "02_resumen_indicadores.png" },
    @{ src = "10_km_lista.png"; dst = "03_historico_kilometros.png" },
    @{ src = "20_rep_lista.png"; dst = "04_repostajes_y_consumo.png" },
    @{ src = "25_rep_tique.png"; dst = "05_lectura_ocr_tique.png" },
    @{ src = "30_proy_1.png"; dst = "06_grafica_proyeccion.png" },
    @{ src = "47_ajustes_multas.png"; dst = "07_consulta_multas_boe.png" },
    @{ src = "60_oscuro.png"; dst = "08_modo_oscuro.png" },
    @{ src = "manual_widget.png"; dst = "09_widget_escritorio.png" }
)

foreach ($item in $shotsToCopy) {
    $srcPath = Join-Path (Join-Path $baseDir "manual") $item.src
    $dstPath = Join-Path $screenshotsDir $item.dst
    if (Test-Path $srcPath) {
        Copy-Item -Path $srcPath -Destination $dstPath -Force
        Write-Output "Copied: $($item.dst)"
    }
}
