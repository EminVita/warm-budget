$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$tools = Join-Path $repo '.tools'
New-Item -ItemType Directory -Force $tools | Out-Null
function Download($url, $file) {
    if (!(Test-Path $file)) {
        & curl.exe -fL --retry 2 --connect-timeout 25 --max-time 900 $url -o "$file.part"
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $url" }
        Move-Item -LiteralPath "$file.part" -Destination $file
    }
}
Download 'https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip' (Join-Path $tools 'jdk.zip')
if (!(Test-Path (Join-Path $tools 'jdk'))) {
    Expand-Archive -LiteralPath (Join-Path $tools 'jdk.zip') -DestinationPath (Join-Path $tools 'jdk')
}
$env:JAVA_HOME = (Get-ChildItem (Join-Path $tools 'jdk') -Directory | Select-Object -First 1).FullName
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Download 'https://services.gradle.org/distributions/gradle-8.13-bin.zip' (Join-Path $tools 'gradle.zip')
if (!(Test-Path (Join-Path $tools 'gradle-8.13'))) { Expand-Archive -LiteralPath (Join-Path $tools 'gradle.zip') -DestinationPath $tools }
Download 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip' (Join-Path $tools 'sdk-tools.zip')
$env:ANDROID_HOME = Join-Path $tools 'android-sdk'
if (!(Test-Path (Join-Path $env:ANDROID_HOME 'cmdline-tools\latest\bin\sdkmanager.bat'))) {
    $unpack = Join-Path $tools 'sdk-unpack'
    Expand-Archive -LiteralPath (Join-Path $tools 'sdk-tools.zip') -DestinationPath $unpack -Force
    New-Item -ItemType Directory -Force (Join-Path $env:ANDROID_HOME 'cmdline-tools') | Out-Null
    Move-Item -LiteralPath (Join-Path $unpack 'cmdline-tools') -Destination (Join-Path $env:ANDROID_HOME 'cmdline-tools\latest')
}
Write-Host 'JDK and Gradle ready. Read Android SDK licenses using sdkmanager --licenses before installing SDK packages.'
Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_HOME=$env:ANDROID_HOME"
