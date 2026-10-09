$ErrorActionPreference = 'Continue' # Native git progress/errors use stderr on Windows PowerShell.
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    git submodule update --init
    if ($LASTEXITCODE -ne 0) { throw 'Submodule checkout failed' }
    python extern/android-reference/tools/fetch_thirdparty.py
    if ($LASTEXITCODE -ne 0) { throw 'SDK dependency fetch failed' }
    foreach ($item in @(
        @{ Repo='extern/android-reference'; Patch='patches/android-sdk.patch' },
        @{ Repo='extern/TheSimpsonsGameRecomp'; Patch='patches/simpsons-android.patch' }
    )) {
        $patch = Join-Path $projectRoot $item.Patch
        git -C $item.Repo apply --reverse --check $patch 2>$null
        if ($LASTEXITCODE -ne 0) {
            git -C $item.Repo apply --check $patch
            if ($LASTEXITCODE -ne 0) { throw "Patch does not match checkout: $($item.Patch)" }
            git -C $item.Repo apply $patch
            if ($LASTEXITCODE -ne 0) { throw "Patch failed: $($item.Patch)" }
        }
    }
} finally { Pop-Location }
