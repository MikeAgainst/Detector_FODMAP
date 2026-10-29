param(
    [string]$Source = '',
    [string]$Res = ''
)

# Regenera los iconos de la app a partir del PNG original (fondo transparente).
# Uso:  powershell -ExecutionPolicy Bypass -File tools\make-icon.ps1
#       powershell -ExecutionPolicy Bypass -File tools\make-icon.ps1 -Source "C:\ruta\icono.png"

Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'

if (-not $Source) {
    $Source = (Get-ChildItem "$env:USERPROFILE\Downloads\*cartoon detective looking through a*.png" |
        Select-Object -First 1).FullName
}
if (-not $Res) {
    $Res = Join-Path (Split-Path $PSScriptRoot -Parent) 'app\src\main\res'
}
if (-not (Test-Path $Source)) { throw "No encuentro la imagen original: $Source" }
if (-not (Test-Path $Res)) { throw "No encuentro la carpeta res: $Res" }

Write-Output "origen:  $Source"
Write-Output "destino: $Res"

$img = [System.Drawing.Image]::FromFile($Source)

# --- 1. Recuadro del dibujo (alpha > 8) sobre una copia reducida ---
$n = 1200
$small = New-Object System.Drawing.Bitmap $n, $n
$gs = [System.Drawing.Graphics]::FromImage($small)
$gs.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gs.DrawImage($img, 0, 0, $n, $n)
$gs.Dispose()

$minX = $n; $minY = $n; $maxX = -1; $maxY = -1
for ($y = 0; $y -lt $n; $y++) {
    for ($x = 0; $x -lt $n; $x++) {
        if ($small.GetPixel($x, $y).A -gt 8) {
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }
}
$small.Dispose()

$scale = $img.Width / $n
$boxX = [int][Math]::Floor($minX * $scale)
$boxY = [int][Math]::Floor($minY * $scale)
$boxW = [int][Math]::Ceiling(($maxX - $minX + 1) * $scale)
$boxH = [int][Math]::Ceiling(($maxY - $minY + 1) * $scale)

# 2 % de margen para no cortar el antialias del borde
$pad = [int]([Math]::Max($boxW, $boxH) * 0.02)
$boxX = [Math]::Max(0, $boxX - $pad)
$boxY = [Math]::Max(0, $boxY - $pad)
$boxW = [Math]::Min($img.Width - $boxX, $boxW + 2 * $pad)
$boxH = [Math]::Min($img.Height - $boxY, $boxH + 2 * $pad)

Write-Output "recorte: x=$boxX y=$boxY w=$boxW h=$boxH  aspecto=$([Math]::Round($boxW / $boxH, 3))"

$srcRect = New-Object System.Drawing.Rectangle $boxX, $boxY, $boxW, $boxH
$aspect = $boxW / $boxH

# Colores del fondo (deben coincidir con @color/ic_launcher_background)
$cream = [System.Drawing.Color]::FromArgb(255, 255, 243, 224)
$creamDark = [System.Drawing.Color]::FromArgb(255, 247, 230, 201)

function New-Canvas([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    return @{ Bitmap = $bmp; Graphics = $g }
}

function Get-RoundedPath([int]$size, [double]$radiusFactor) {
    $r = [double]$size * $radiusFactor
    $d = 2 * $r
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddArc(0, 0, $d, $d, 180, 90)
    $path.AddArc($size - $d, 0, $d, $d, 270, 90)
    $path.AddArc($size - $d, $size - $d, $d, $d, 0, 90)
    $path.AddArc(0, $size - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

# Dibuja el recorte encajado en una caja cuadrada de lado "size * boxFactor"
function Get-ArtRect([int]$size, [double]$boxFactor) {
    $side = $size * $boxFactor
    $artH = $side
    $artW = $side * $aspect
    if ($artW -gt $side) { $artW = $side; $artH = $side / $aspect }
    $dw = [int][Math]::Round($artW)
    $dh = [int][Math]::Round($artH)
    $dx = [int](($size - $dw) / 2)
    $dy = [int](($size - $dh) / 2)
    return New-Object System.Drawing.Rectangle -ArgumentList $dx, $dy, $dw, $dh
}

function Draw-Art($g, [int]$size, [double]$boxFactor) {
    $dest = Get-ArtRect $size $boxFactor
    $g.DrawImage($img, $dest, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
}

# --- 2. Icono clásico (Android 7): cuadrado redondeado con fondo crema ---
$legacy = [ordered]@{ 'mdpi' = 48; 'hdpi' = 72; 'xhdpi' = 96; 'xxhdpi' = 144; 'xxxhdpi' = 192 }
foreach ($density in $legacy.Keys) {
    $size = $legacy[$density]
    $c = New-Canvas $size
    $g = $c.Graphics
    $path = Get-RoundedPath $size 0.19
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Point 0, 0),
        (New-Object System.Drawing.Point $size, $size),
        $cream, $creamDark)
    $g.FillPath($brush, $path)
    $brush.Dispose()
    $path.Dispose()
    Draw-Art $g $size 0.74
    $dir = Join-Path $Res "mipmap-$density"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $g.Dispose()
    $c.Bitmap.Save((Join-Path $dir 'ic_launcher.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $c.Bitmap.Dispose()
    Write-Output "normal     $density ($size px)"
}

# --- 3. Icono redondo (lanzadores circulares) ---
foreach ($density in $legacy.Keys) {
    $size = $legacy[$density]
    $c = New-Canvas $size
    $g = $c.Graphics
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Point 0, 0),
        (New-Object System.Drawing.Point $size, $size),
        $cream, $creamDark)
    $g.FillEllipse($brush, 0, 0, $size, $size)
    $brush.Dispose()
    Draw-Art $g $size 0.66
    $dir = Join-Path $Res "mipmap-$density"
    $g.Dispose()
    $c.Bitmap.Save((Join-Path $dir 'ic_launcher_round.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $c.Bitmap.Dispose()
    Write-Output "redondo    $density ($size px)"
}

# --- 4. Capa frontal del icono adaptativo (108 dp, arte en la zona segura de 66 dp) ---
$fore = [ordered]@{ 'mdpi' = 108; 'hdpi' = 162; 'xhdpi' = 216; 'xxhdpi' = 324; 'xxxhdpi' = 432 }
foreach ($density in $fore.Keys) {
    $size = $fore[$density]
    $c = New-Canvas $size
    Draw-Art $c.Graphics $size 0.60
    $dir = Join-Path $Res "drawable-$density"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $c.Graphics.Dispose()
    $c.Bitmap.Save((Join-Path $dir 'ic_launcher_foreground.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $c.Bitmap.Dispose()
    Write-Output "foreground $density ($size px)"
}

# --- 5. Silueta monocroma para iconos tematizados (Android 13+) ---
$whiteMatrix = New-Object System.Drawing.Imaging.ColorMatrix
$whiteMatrix.Matrix00 = 0; $whiteMatrix.Matrix11 = 0; $whiteMatrix.Matrix22 = 0
$whiteMatrix.Matrix33 = 1; $whiteMatrix.Matrix44 = 0
$whiteMatrix.Matrix40 = 1; $whiteMatrix.Matrix41 = 1; $whiteMatrix.Matrix42 = 1
$whiteAttr = New-Object System.Drawing.Imaging.ImageAttributes
$whiteAttr.SetColorMatrix($whiteMatrix)

foreach ($density in $fore.Keys) {
    $size = $fore[$density]
    $c = New-Canvas $size
    $dest = Get-ArtRect $size 0.60
    $c.Graphics.DrawImage($img, $dest, $srcRect.X, $srcRect.Y, $srcRect.Width, $srcRect.Height,
        [System.Drawing.GraphicsUnit]::Pixel, $whiteAttr)
    $dir = Join-Path $Res "drawable-$density"
    $c.Graphics.Dispose()
    $c.Bitmap.Save((Join-Path $dir 'ic_launcher_monochrome.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    $c.Bitmap.Dispose()
    Write-Output "monocromo  $density ($size px)"
}

$img.Dispose()
Write-Output "Listo."
