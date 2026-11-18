param([string[]]$Tasks = @(':core:test', ':app:testDebugUnitTest', ':app:assembleDebug', ':app:assembleRelease', ':app:lintDebug'))
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$jdk = Get-ChildItem (Join-Path $repo '.tools\jdk') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to JDK 17 or run scripts/bootstrap.ps1.' }
$env:PATH = "$env:JAVA_HOME\bin;" + ($env:PATH -replace '"', '')
# Ignore legacy machine-wide Java 8 classpaths; only affect this build process.
Remove-Item Env:CLASSPATH -ErrorAction SilentlyContinue
$env:GRADLE_USER_HOME = Join-Path $repo '.tools\gradle-home'
$env:ANDROID_HOME = Join-Path $repo '.tools\android-sdk'
$gradle = Join-Path $repo '.tools\gradle-8.13\bin\gradle.bat'
if (!(Test-Path $gradle)) { throw 'Run scripts/bootstrap.ps1 or install Gradle 8.13.' }
Push-Location $repo
try {
    $jvmOptions = "-Dorg.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8 -Djava.library.path=$env:SystemRoot/System32"
    & $gradle @Tasks '--console=plain' $jvmOptions
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed ($LASTEXITCODE)." }
}
finally { Pop-Location }
