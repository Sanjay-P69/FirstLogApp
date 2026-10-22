param([ValidateSet('debug', 'release', 'check', 'reproduce')][string]$Mode = 'debug')
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

# Parse data only: never execute a shared .env as PowerShell code.
$allowed = @('JAVA_HOME', 'ANDROID_HOME', 'ANDROID_KEYSTORE_PATH',
    'ANDROID_KEYSTORE_PASSWORD', 'ANDROID_KEY_ALIAS', 'ANDROID_KEY_PASSWORD')
if (Test-Path '.env') {
    foreach ($line in Get-Content '.env') {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
        $pair = $line.Split('=', 2)
        # Administrative token is never passed to Gradle or needed for builds.
        if ($pair[0] -eq 'FATHER_ACCOUNT_GH') { continue }
        if ($pair.Count -ne 2 -or $pair[0] -notin $allowed -or !$pair[1]) {
            throw 'Invalid .env entry. Use only the documented KEY=value entries.'
        }
        [Environment]::SetEnvironmentVariable($pair[0], $pair[1], 'Process')
    }
}
if (!(Test-Path "$env:JAVA_HOME/bin/java.exe")) { throw 'Set JAVA_HOME in .env to your Temurin 21.0.8+9 installation.' }
if (!(Test-Path "$env:ANDROID_HOME/platforms/android-36/android.jar")) { throw 'Install Android SDK platform 36; set ANDROID_HOME in .env.' }
if (!(Test-Path "$env:ANDROID_HOME/build-tools/36.0.0/apksigner.bat")) { throw 'Install Android SDK Build-Tools 36.0.0.' }
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
if ($env:ANDROID_KEYSTORE_PATH -and (Test-Path $env:ANDROID_KEYSTORE_PATH)) {
    $env:ANDROID_KEYSTORE_PATH = (Resolve-Path $env:ANDROID_KEYSTORE_PATH).Path
}
$env:TZ = 'UTC'
$env:LANG = 'en_US.UTF-8'
if ($Mode -eq 'reproduce') {
    python scripts/verify_reproducible.py
    if ($LASTEXITCODE -ne 0) { throw 'Reproducibility verification failed.' }
    exit 0
}
if ($Mode -eq 'release') {
    foreach ($name in $allowed | Where-Object { $_.StartsWith('ANDROID_KEY') }) {
        if (![Environment]::GetEnvironmentVariable($name)) { throw "Missing $name in private .env." }
    }
    if (!(Test-Path $env:ANDROID_KEYSTORE_PATH)) { throw 'Release keystore file not found. Restore the original shared key; do not create another.' }
}
$tasks = switch ($Mode) {
    'debug' { @(':app:assembleDebug') }
    'release' { @(':app:assembleRelease') }
    'check' { @('spotlessCheck', 'detekt', ':app:assembleDebug', ':app:assembleRelease', ':app:assembleDebugAndroidTest', ':app:testDebugUnitTest', ':app:testReleaseUnitTest', ':app:lintDebug', ':app:lintRelease') }
}
& ./gradlew.bat @tasks --no-configuration-cache --stacktrace
if ($LASTEXITCODE -ne 0) { throw 'Build failed. Read the first error above; do not share secret files or passwords in logs.' }
if ($Mode -eq 'release') {
    & "$env:ANDROID_HOME/build-tools/36.0.0/apksigner.bat" verify --verbose app/build/outputs/apk/release/app-release.apk
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
}
Write-Host 'Success. APKs are in app/build/outputs/apk/. Open the debug or release folder.'
