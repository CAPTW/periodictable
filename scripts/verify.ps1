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

function Remove-GeneratedDirs {
    $targets = @(
        (Join-Path $env:PROJECT_ROOT "app\build\generated\ksp"),
        (Join-Path $env:PROJECT_ROOT "app\build\generated\hilt"),
        (Join-Path $env:PROJECT_ROOT "app\build\generated\ap_generated_sources")
    )

    foreach ($target in $targets) {
        if (Test-Path $target) {
            cmd /c "attrib -R `"$target`" /S /D >nul 2>&1" | Out-Null
            cmd /c "rd /s /q `"$target`" >nul 2>&1" | Out-Null
        }
    }
}

function Purge-BuildDirs {
    $targets = @(
        (Join-Path $env:PROJECT_ROOT "app\build"),
        (Join-Path $env:PROJECT_ROOT "build")
    )

    foreach ($target in $targets) {
        if (Test-Path $target) {
            cmd /c "attrib -R `"$target`" /S /D >nul 2>&1" | Out-Null
            cmd /c "rd /s /q `"$target`" >nul 2>&1" | Out-Null
        }
    }
}

Push-Location $env:PROJECT_ROOT
try {
    Write-Host "`n=== validate assets ===" -ForegroundColor Cyan
    if (Get-Command python -ErrorAction SilentlyContinue) {
        python scripts/validate_assets.py
    }
    elseif (Get-Command py -ErrorAction SilentlyContinue) {
        py scripts/validate_assets.py
    }
    else {
        throw "python runtime not found for asset validation"
    }
    if ($LASTEXITCODE -ne 0) { throw "asset validation failed (exit $LASTEXITCODE)" }

    Write-Host "`n=== clean (filesystem purge) ===" -ForegroundColor Cyan
    Purge-BuildDirs

    Remove-GeneratedDirs
    Write-Host "`n=== lintRelease + testReleaseUnitTest + assembleRelease ===" -ForegroundColor Cyan
    & $GradleExe --no-daemon lintRelease testReleaseUnitTest assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "lintRelease/testReleaseUnitTest/assembleRelease failed (exit $LASTEXITCODE)" }

    Write-Host "`n✅ verify PASS" -ForegroundColor Green
}
catch {
    Write-Host "`n❌ verify FAIL: $_" -ForegroundColor Red
    exit 1
}
finally {
    Pop-Location
}
