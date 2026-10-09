param([Parameter(Mandatory=$true)][string]$GameFolder)
$ErrorActionPreference = 'Continue' # adb prints transfer progress to stderr.
$source = (Resolve-Path -LiteralPath $GameFolder -ErrorAction Stop).Path
if (-not (Test-Path -LiteralPath (Join-Path $source 'default.xex'))) {
    throw 'Select the game folder containing default.xex at its root.'
}
$sharedRoot = (adb shell printenv EXTERNAL_STORAGE)
if ($LASTEXITCODE -ne 0 -or -not $sharedRoot) { throw 'Connect one authorized Android device.' }
$sharedRoot = $sharedRoot.Trim()
if ($sharedRoot -notmatch '^/(sdcard|storage/emulated/[0-9]+)$') {
    throw "Unrecognized shared-storage location: $sharedRoot"
}
$destination = "$sharedRoot/TSG"
adb shell mkdir -p $destination
if ($LASTEXITCODE -ne 0) { throw 'Could not create TSG folder.' }
adb push (Join-Path $source '.') "$destination/"
if ($LASTEXITCODE -ne 0) { throw 'Game transfer failed.' }
Write-Host "Data copied to $destination. Select TSG in the APK folder picker."
