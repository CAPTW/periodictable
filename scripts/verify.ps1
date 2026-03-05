# verify.ps1 — Gradle 빌드 검증 (PowerShell)
$ErrorActionPreference = "Stop"

. (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "env.ps1")

# Gradle 실행 파일 결정
if ($env:GRADLE_CMD -and (Test-Path $env:GRADLE_CMD)) {
    $GradleExe = $env:GRADLE_CMD
}
elseif (Test-Path (Join-Path $env:PROJECT_ROOT "gradlew.bat")) {
    $GradleExe = Join-Path $env:PROJECT_ROOT "gradlew.bat"
}
elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
    $GradleExe = (Get-Command gradle).Source
}
else {
    Write-Error "Neither gradlew.bat nor gradle was found. Install JDK 17 and Gradle or use the wrapper."
    exit 2
}

$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

Write-Host "▶ Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Cyan
Write-Host "▶ Using Gradle: $GradleExe" -ForegroundColor Cyan
Write-Host "▶ PROJECT_ROOT: $env:PROJECT_ROOT" -ForegroundColor Cyan

Push-Location $env:PROJECT_ROOT
try {
    Write-Host "`n=== clean ===" -ForegroundColor Cyan
    & $GradleExe --no-daemon clean
    if ($LASTEXITCODE -ne 0) { throw "clean failed (exit $LASTEXITCODE)" }

    Write-Host "`n=== lint + test + assembleDebug ===" -ForegroundColor Cyan
    & $GradleExe --no-daemon lint test assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "lint/test/assembleDebug failed (exit $LASTEXITCODE)" }

    Write-Host "`n✅ verify PASS" -ForegroundColor Green
}
catch {
    Write-Host "`n❌ verify FAIL: $_" -ForegroundColor Red
    exit 1
}
finally {
    Pop-Location
}
