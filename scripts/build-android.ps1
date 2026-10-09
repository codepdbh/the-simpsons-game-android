param([ValidateSet('Debug','Release')][string]$Configuration = 'Debug')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$sdkRoot = $env:ANDROID_HOME
if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_SDK_ROOT }
if (-not $sdkRoot) { $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
if (-not (Test-Path -LiteralPath (Join-Path $sdkRoot 'ndk\28.2.13676358'))) { throw 'Install NDK 28.2.13676358 using Android Studio SDK Manager.' }
if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'C:\Program Files\Android\Android Studio\jbr')) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
$env:ANDROID_HOME = $sdkRoot
if (-not (Test-Path -LiteralPath (Join-Path $projectRoot 'extern\SDL\CMakeLists.txt'))) {
    git clone --depth 1 --branch release-3.2.28 https://github.com/libsdl-org/SDL.git (Join-Path $projectRoot 'extern\SDL')
    if ($LASTEXITCODE -ne 0) { throw 'SDL clone failed' }
}
Push-Location $projectRoot
try {
    $ErrorActionPreference = 'Continue' # Windows PowerShell treats native stderr as errors.
    & .\gradlew.bat "assemble$Configuration"
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
} finally { Pop-Location }
