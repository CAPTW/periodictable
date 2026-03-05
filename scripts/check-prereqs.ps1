# check-prereqs.ps1 — 사전 조건 확인
$ErrorActionPreference = "Stop"

. (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "env.ps1")

function Fail($msg) { Write-Error "❌ $msg"; exit 2 }
function Ok($msg) { Write-Host "✅ $msg" }
function Warn($msg) { Write-Warning "⚠️ $msg" }

# git 확인
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Fail "git not found (required)" }

# java 확인
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Warn "java not found — Gradle/Android builds may fail. Install JDK 17+."
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
    # JDK 11+ 필요 체크
    if ($javaVerStr -match '"1\.[0-8]\.') {
        Warn "JDK version < 11 detected. AGP 8.3.2 requires JDK 11+, project targets JDK 17."
        Warn "Set JAVA_HOME to JDK 17 installation. Download: https://adoptium.net/"
    }
}

# gradlew 확인
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

# git repo 체크
try {
    git rev-parse --is-inside-work-tree 2>$null | Out-Null
    Ok "git repo detected"
}
catch {
    Warn "Not a git repo. Swarm will fail. Ralph Loop can still run."
}

Ok "prereqs check done"
