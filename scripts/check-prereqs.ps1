# Check local prerequisites for building and verifying the Android app.
$ErrorActionPreference = "Stop"

. (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "env.ps1")

function Fail($msg) { Write-Error "ERROR: $msg"; exit 2 }
function Ok($msg) { Write-Host "OK: $msg" }
function Warn($msg) { Write-Warning "WARN: $msg" }

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Fail "git not found"
}

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Warn "java not found; Gradle/Android builds may fail. Install JDK 21."
}
else {
    try {
        $ErrorActionPreference = "SilentlyContinue"
        $javaVer = (& java -version 2>&1) | Select-Object -First 1
        $ErrorActionPreference = "Stop"
    }
    catch {
        $ErrorActionPreference = "Stop"
        $javaVer = "unknown"
    }

    $javaVerStr = "$javaVer"
    Ok "java found: $javaVerStr"
    if ($javaVerStr -match '"1\.[0-9]\.' -or $javaVerStr -match '"1[0-9]\.') {
        Warn "JDK version < 21 detected. Set JAVA_HOME to a JDK 21 installation."
    }
}

$gradlew = Join-Path $env:PROJECT_ROOT "gradlew.bat"
if (Test-Path $gradlew) {
    $env:GRADLE_CMD = $gradlew
    Ok "using Gradle wrapper: $gradlew"
}
elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
    $env:GRADLE_CMD = (Get-Command gradle).Source
    Ok "gradlew unavailable; using system gradle: $($env:GRADLE_CMD)"
}
else {
    Fail "Neither gradlew.bat nor gradle was found."
}

try {
    git rev-parse --is-inside-work-tree 2>$null | Out-Null
    Ok "git repo detected"
}
catch {
    Warn "not a git repo; build verification can still run, but repository checks are unavailable"
}

Ok "prereqs check done"
