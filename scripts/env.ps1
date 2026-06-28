# PeriodicTable project environment settings.
$ErrorActionPreference = "Stop"

$script:SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$script:PROJECT_ROOT = (Resolve-Path (Join-Path $SCRIPT_DIR "..")).Path
$env:PROJECT_ROOT = $script:PROJECT_ROOT

if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $script:PROJECT_ROOT ".gradle-user-home" }
if (-not $env:ANDROID_USER_HOME) { $env:ANDROID_USER_HOME = Join-Path $script:PROJECT_ROOT ".android-home" }

New-Item -ItemType Directory -Path $env:GRADLE_USER_HOME -Force -ErrorAction SilentlyContinue | Out-Null
New-Item -ItemType Directory -Path $env:ANDROID_USER_HOME -Force -ErrorAction SilentlyContinue | Out-Null
