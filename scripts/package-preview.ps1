$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$jdk = Get-ChildItem (Join-Path $repo '.tools\jdk') -Directory | Select-Object -First 1
$env:JAVA_HOME = $jdk.FullName
$env:PATH = "$env:JAVA_HOME\bin;" + ($env:PATH -replace '"','')
$output = Join-Path $repo 'dist'
New-Item -ItemType Directory -Force $output | Out-Null
$key = Join-Path $env:USERPROFILE '.android\debug.keystore'
if (!(Test-Path $key)) { throw 'Build assembleDebug first to create the local test key.' }
$signer = Join-Path $repo '.tools\android-sdk\build-tools\36.0.0\apksigner.bat'
$apk = Join-Path $output 'living-budget-0.2.0-preview.apk'
& $signer sign --ks $key --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out $apk (Join-Path $repo 'app\build\outputs\apk\release\app-release-unsigned.apk')
if ($LASTEXITCODE -ne 0) { throw 'Preview signing failed.' }
& $signer verify --verbose $apk
if ($LASTEXITCODE -ne 0) { throw 'Signature verification failed.' }
Get-Item $apk | Select-Object FullName,Length
Get-FileHash $apk -Algorithm SHA256 | Select-Object Hash
