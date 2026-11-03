param()

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$desktopRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$outputDirectory = Join-Path $desktopRoot 'build'
$outputPath = Join-Path $outputDirectory 'icon.png'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$bitmap = [System.Drawing.Bitmap]::new(512, 512)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$graphics.Clear([System.Drawing.Color]::Transparent)

$shape = [System.Drawing.Drawing2D.GraphicsPath]::new()
$shape.AddArc(24, 24, 112, 112, 180, 90)
$shape.AddArc(376, 24, 112, 112, 270, 90)
$shape.AddArc(376, 376, 112, 112, 0, 90)
$shape.AddArc(24, 376, 112, 112, 90, 90)
$shape.CloseFigure()

$background = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(15, 118, 110))
$graphics.FillPath($background, $shape)

$accent = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(239, 131, 84))
$graphics.FillEllipse($accent, 382, 50, 78, 78)

$font = [System.Drawing.Font]::new('Microsoft YaHei', 286, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$foreground = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
$format = [System.Drawing.StringFormat]::new()
$format.Alignment = [System.Drawing.StringAlignment]::Center
$format.LineAlignment = [System.Drawing.StringAlignment]::Center
$iconCharacter = ([char]0x95F2).ToString()
$graphics.DrawString($iconCharacter, $font, $foreground, [System.Drawing.RectangleF]::new(20, 28, 472, 456), $format)

$bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)

$format.Dispose()
$foreground.Dispose()
$font.Dispose()
$accent.Dispose()
$background.Dispose()
$shape.Dispose()
$graphics.Dispose()
$bitmap.Dispose()

Write-Host "Desktop icon created: $outputPath"
