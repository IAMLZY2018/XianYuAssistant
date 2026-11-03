param(
    [ValidateSet('all', 'nsis', 'portable')]
    [string]$Target = 'all',
    [switch]$UseChinaMirror
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$desktopRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $desktopRoot '..'))
$cacheRoot = Join-Path $desktopRoot '.cache'
$resourcesRoot = Join-Path $desktopRoot 'resources'
$backendRoot = Join-Path $resourcesRoot 'backend'
$runtimeRoot = Join-Path $resourcesRoot 'runtime'
$jdkRoot = Join-Path $cacheRoot 'jdk-21'

function Assert-ChildPath {
    param([string]$Path, [string]$Parent)
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $fullParent = [System.IO.Path]::GetFullPath($Parent).TrimEnd('\') + '\'
    if (-not $fullPath.StartsWith($fullParent, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify path outside build root: $fullPath"
    }
}

function Reset-BuildDirectory {
    param([string]$Path)
    Assert-ChildPath -Path $Path -Parent $desktopRoot
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $Path | Out-Null
}

function Test-ZipArchive {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return $false
    }
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
        $zip.Dispose()
        return $true
    }
    catch {
        return $false
    }
}

function Resolve-JavaDownloadUri {
    param([string]$ImageType)
    if (-not $UseChinaMirror) {
        return "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/$ImageType/hotspot/normal/eclipse?project=jdk"
    }

    $mirrorRoot = "https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/$ImageType/x64/windows/"
    $listing = (Invoke-WebRequest -Uri $mirrorRoot -UseBasicParsing).Content
    $pattern = 'OpenJDK21U-{0}_x64_windows_hotspot_[^"''<>\s]+\.zip' -f $ImageType
    $fileName = [regex]::Matches($listing, $pattern) |
        ForEach-Object { $_.Value } |
        Select-Object -Unique |
        Select-Object -Last 1
    if ([string]::IsNullOrWhiteSpace($fileName)) {
        throw "Could not resolve the latest Temurin $ImageType archive from $mirrorRoot"
    }
    return "$mirrorRoot$fileName"
}

function Get-PortableJava {
    param(
        [ValidateSet('jdk', 'jre')]
        [string]$ImageType,
        [string]$Destination
    )

    $javaExecutable = Join-Path $Destination 'bin\java.exe'
    if (Test-Path -LiteralPath $javaExecutable) {
        return
    }

    $archive = Join-Path $cacheRoot "temurin-21-$ImageType-windows-x64.zip"
    if (-not (Test-ZipArchive -Path $archive)) {
        if (Test-Path -LiteralPath $archive) {
            Remove-Item -LiteralPath $archive -Force
        }
        $uri = Resolve-JavaDownloadUri -ImageType $ImageType
        $partialArchive = "$archive.part"
        if (Test-Path -LiteralPath $partialArchive) {
            Remove-Item -LiteralPath $partialArchive -Force
        }
        Write-Host "Downloading Temurin Java 21 $ImageType..."
        $curl = Get-Command 'curl.exe' -ErrorAction SilentlyContinue
        if ($null -ne $curl) {
            & $curl.Source --location --fail --retry 3 --output $partialArchive $uri
            if ($LASTEXITCODE -ne 0) {
                throw "Failed to download Java archive: $uri"
            }
        }
        else {
            Invoke-WebRequest -Uri $uri -OutFile $partialArchive -UseBasicParsing
        }
        Move-Item -LiteralPath $partialArchive -Destination $archive
        if (-not (Test-ZipArchive -Path $archive)) {
            throw "Downloaded Java archive is invalid: $archive"
        }
    }

    $extractRoot = Join-Path $cacheRoot "extract-$ImageType"
    Reset-BuildDirectory -Path $extractRoot
    Expand-Archive -LiteralPath $archive -DestinationPath $extractRoot -Force
    $archiveRoot = Get-ChildItem -LiteralPath $extractRoot -Directory | Select-Object -First 1
    if ($null -eq $archiveRoot) {
        throw "Java archive did not contain a root directory: $archive"
    }

    Assert-ChildPath -Path $Destination -Parent $desktopRoot
    if (Test-Path -LiteralPath $Destination) {
        Remove-Item -LiteralPath $Destination -Recurse -Force
    }
    Move-Item -LiteralPath $archiveRoot.FullName -Destination $Destination
    Remove-Item -LiteralPath $extractRoot -Recurse -Force
}

function Invoke-Checked {
    param([string]$Executable, [string[]]$Arguments)
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $Executable $($Arguments -join ' ')"
    }
}

New-Item -ItemType Directory -Force -Path $cacheRoot, $resourcesRoot | Out-Null
Get-PortableJava -ImageType 'jdk' -Destination $jdkRoot
Get-PortableJava -ImageType 'jre' -Destination $runtimeRoot

$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:Path
$originalElectronMirror = $env:ELECTRON_MIRROR
$originalBuilderMirror = $env:ELECTRON_BUILDER_BINARIES_MIRROR
$originalPlaywrightHost = $env:PLAYWRIGHT_DOWNLOAD_HOST

try {
    $env:JAVA_HOME = $jdkRoot
    $env:Path = "$(Join-Path $jdkRoot 'bin');$originalPath"

    if ($UseChinaMirror) {
        $env:ELECTRON_MIRROR = 'https://npmmirror.com/mirrors/electron/'
        $env:ELECTRON_BUILDER_BINARIES_MIRROR = 'https://npmmirror.com/mirrors/electron-builder-binaries/'
        $env:PLAYWRIGHT_DOWNLOAD_HOST = 'https://npmmirror.com/mirrors/playwright'
    }

    Write-Host 'Installing and building Vue frontend...'
    Push-Location (Join-Path $projectRoot 'vue-code')
    try {
        Invoke-Checked -Executable 'npm.cmd' -Arguments @('ci')
        Invoke-Checked -Executable 'npm.cmd' -Arguments @('run', 'build')
    }
    finally {
        Pop-Location
    }

    Write-Host 'Building Spring Boot backend...'
    Push-Location $projectRoot
    try {
        Invoke-Checked -Executable 'mvn.cmd' -Arguments @('--batch-mode', 'clean', 'package')
        Reset-BuildDirectory -Path $backendRoot
        Copy-Item -LiteralPath (Join-Path $projectRoot 'target\XianYuAssistant-2.0.3.jar') -Destination (Join-Path $backendRoot 'app.jar')

        $classPathFile = Join-Path $cacheRoot 'maven-classpath.txt'
        Invoke-Checked -Executable 'mvn.cmd' -Arguments @(
            '--batch-mode',
            'dependency:build-classpath',
            "-Dmdep.outputFile=$classPathFile",
            '-Dmdep.pathSeparator=;'
        )
        $playwrightClassPath = (Get-Content -Raw -LiteralPath $classPathFile).Trim()
        $env:PLAYWRIGHT_BROWSERS_PATH = Join-Path $backendRoot 'ms-playwright'
        Write-Host 'Installing Playwright Chromium...'
        Invoke-Checked -Executable (Join-Path $jdkRoot 'bin\java.exe') -Arguments @(
            '-cp',
            $playwrightClassPath,
            'com.microsoft.playwright.CLI',
            'install',
            'chromium'
        )
    }
    finally {
        Pop-Location
    }

    Write-Host 'Installing Electron dependencies and running unit tests...'
    Push-Location $desktopRoot
    try {
        Invoke-Checked -Executable 'npm.cmd' -Arguments @('ci')
        Invoke-Checked -Executable 'npm.cmd' -Arguments @('test')
        Invoke-Checked -Executable 'powershell.exe' -Arguments @(
            '-NoProfile',
            '-ExecutionPolicy',
            'Bypass',
            '-File',
            (Join-Path $PSScriptRoot 'create-icon.ps1')
        )

        if ($Target -eq 'all') {
            Invoke-Checked -Executable 'npx.cmd' -Arguments @('--no-install', 'electron-builder', '--win', '--x64')
        }
        else {
            Invoke-Checked -Executable 'npx.cmd' -Arguments @('--no-install', 'electron-builder', '--win', $Target, '--x64')
        }
    }
    finally {
        Pop-Location
    }
}
finally {
    $env:JAVA_HOME = $originalJavaHome
    $env:Path = $originalPath
    $env:ELECTRON_MIRROR = $originalElectronMirror
    $env:ELECTRON_BUILDER_BINARIES_MIRROR = $originalBuilderMirror
    $env:PLAYWRIGHT_DOWNLOAD_HOST = $originalPlaywrightHost
}

Write-Host "Desktop artifacts are available in: $(Join-Path $desktopRoot 'dist')"
