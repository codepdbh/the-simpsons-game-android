$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$validationRoot = Join-Path $projectRoot 'artifacts\validation'
New-Item -ItemType Directory -Force $validationRoot | Out-Null
$url = 'https://github.com/KhronosGroup/Vulkan-ValidationLayers/releases/download/vulkan-sdk-1.4.363.0/android-binaries-1.4.363.0.zip'
$archive = Join-Path $validationRoot 'layers.zip'
Invoke-WebRequest $url -OutFile $archive
Expand-Archive -LiteralPath $archive -DestinationPath (Join-Path $validationRoot 'unpacked') -Force
$layer = Get-ChildItem (Join-Path $validationRoot 'unpacked') -Recurse -Filter libVkLayer_khronos_validation.so | Where-Object FullName -Match 'arm64-v8a'
if (@($layer).Count -ne 1) { throw 'Expected one ARM64 validation layer' }
$destination = Join-Path $projectRoot 'app\src\debug\jniLibs\arm64-v8a'
New-Item -ItemType Directory -Force $destination | Out-Null
Copy-Item -LiteralPath $layer.FullName -Destination $destination
Write-Host 'Debug validation ready. Rebuild Debug; Release is unaffected.'
