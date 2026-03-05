# env.ps1 — PeriodicTable 프로젝트 환경 설정
$ErrorActionPreference = "Stop"

$script:SCRIPT_DIR  = Split-Path -Parent $MyInvocation.MyCommand.Path
$script:PROJECT_ROOT = (Resolve-Path (Join-Path $SCRIPT_DIR "..")).Path
$env:PROJECT_ROOT    = $script:PROJECT_ROOT

# Workspace-local caches (sandbox friendly)
if (-not $env:GRADLE_USER_HOME)   { $env:GRADLE_USER_HOME   = Join-Path $script:PROJECT_ROOT ".gradle-user-home" }
if (-not $env:ANDROID_USER_HOME)  { $env:ANDROID_USER_HOME  = Join-Path $script:PROJECT_ROOT ".android-home" }

New-Item -ItemType Directory -Path $env:GRADLE_USER_HOME  -Force -ErrorAction SilentlyContinue | Out-Null
New-Item -ItemType Directory -Path $env:ANDROID_USER_HOME -Force -ErrorAction SilentlyContinue | Out-Null
New-Item -ItemType Directory -Path (Join-Path $script:PROJECT_ROOT "agent\logs") -Force -ErrorAction SilentlyContinue | Out-Null
