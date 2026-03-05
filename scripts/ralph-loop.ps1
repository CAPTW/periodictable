# ralph-loop.ps1 — Ralph Loop (PowerShell)
# Verify → 실패 로그 저장 → 다음 iteration 반복
# 사용법: .\scripts\ralph-loop.ps1 [-MaxIters 12] [-AutoCommit] [-DryRun]
param(
    [int]$MaxIters = $(if ($env:MAX_ITERS) { [int]$env:MAX_ITERS }   else { 12 }),
    [switch]$AutoCommit = $(if ($env:AUTO_COMMIT -eq "true") { $true } else { $false }),
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

. (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "env.ps1")

# prereqs (경고만, 중단하지 않음)
try { & (Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "check-prereqs.ps1") }
catch { Write-Warning "check-prereqs warning: $_" }

$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

$VERIFY_SCRIPT = Join-Path $PSScriptRoot "verify.ps1"
$LogDir = Join-Path $env:PROJECT_ROOT "agent\logs"
New-Item -ItemType Directory -Path $LogDir -Force -ErrorAction SilentlyContinue | Out-Null

Write-Host ""
Write-Host "▶ Ralph Loop start" -ForegroundColor Cyan
Write-Host "   - MAX_ITERS:  $MaxIters"
Write-Host "   - AUTO_COMMIT: $AutoCommit"
Write-Host "   - DRY_RUN:    $DryRun"
Write-Host "   - logs:       $LogDir"
Write-Host ""

$PromptFile = Join-Path $env:PROJECT_ROOT "agent\prompts\ralph-loop.md"
if (-not (Test-Path $PromptFile)) {
    Write-Error "Prompt file not found: $PromptFile"
    exit 2
}
$BasePrompt = Get-Content $PromptFile -Raw

for ($i = 1; $i -le $MaxIters; $i++) {
    Write-Host ""
    Write-Host "=== Iteration $i / $MaxIters ===" -ForegroundColor Yellow

    # ── 1) verify 시도 ──
    $verifyLog = Join-Path $LogDir "verify-$i.txt"
    $verifyPassed = $false

    try {
        $verifyOutput = & $VERIFY_SCRIPT 2>&1
        $verifyPassed = ($LASTEXITCODE -eq 0)
        $verifyOutput | Out-File -FilePath $verifyLog -Encoding utf8
    }
    catch {
        "EXCEPTION: $_" | Out-File -FilePath $verifyLog -Encoding utf8
    }

    if ($verifyPassed) {
        Write-Host "✅ verify PASS — loop 종료" -ForegroundColor Green
        exit 0
    }

    Write-Host "❌ verify FAIL — 로그: $verifyLog" -ForegroundColor Red

    # ── 2) git context 수집 ──
    $statusOut = ""
    $diffStatOut = ""
    try {
        git rev-parse --is-inside-work-tree 2>$null | Out-Null
        $statusOut = git status --porcelain 2>$null
        $diffStatOut = git diff --stat 2>$null
    }
    catch {}

    $statusFile = Join-Path $LogDir "git-status-$i.txt"
    $diffStatFile = Join-Path $LogDir "git-diffstat-$i.txt"
    $statusOut   | Out-File -FilePath $statusFile   -Encoding utf8
    $diffStatOut | Out-File -FilePath $diffStatFile -Encoding utf8

    # ── 3) verify 로그 tail ──
    $verifyTail = ""
    if (Test-Path $verifyLog) {
        $lines = Get-Content $verifyLog -Tail 220 -ErrorAction SilentlyContinue
        $verifyTail = ($lines -join "`n")
    }

    # ── 4) Codex / AI 에이전트에 전달할 프롬프트 구성 ──
    $prompt = @"
$BasePrompt

## Context
- iteration: $i / $MaxIters

### git status (porcelain)
$statusOut

### git diff --stat
$diffStatOut

### verify output (tail 220)
$verifyTail
"@

    $runLog = Join-Path $LogDir "codex-$i.md"
    $prompt | Out-File -FilePath $runLog -Encoding utf8

    if ($DryRun) {
        Write-Host "🔍 DRY RUN — prompt saved to: $runLog" -ForegroundColor Magenta
    }
    else {
        # codex exec 호출 (설치된 경우)
        if (Get-Command codex -ErrorAction SilentlyContinue) {
            Write-Host "🤖 Codex에게 수정 요청..." -ForegroundColor Cyan
            try {
                codex exec --full-auto --ephemeral $prompt 2>&1 | Tee-Object -FilePath $runLog
            }
            catch {
                Write-Warning "codex exec failed: $_"
                "CODEX ERROR: $_" | Out-File -FilePath $runLog -Append -Encoding utf8
            }
        }
        else {
            Write-Host "⚠️ codex CLI not found — prompt saved to: $runLog" -ForegroundColor Yellow
            Write-Host "   수동으로 프롬프트를 AI 에이전트에 전달하세요."
        }
    }

    # ── 5) Auto-commit (옵션) ──
    if ($AutoCommit) {
        try {
            git rev-parse --is-inside-work-tree 2>$null | Out-Null
            git add -A
            git commit -m "codex: ralph-loop iteration $i" --allow-empty 2>$null
            Write-Host "📝 Auto-committed iteration $i" -ForegroundColor DarkGray
        }
        catch { Write-Warning "auto-commit skipped: $_" }
    }
}

Write-Host ""
Write-Host "🛑 MAX_ITERS ($MaxIters) 도달 — 실패 로그 확인: $LogDir" -ForegroundColor Red
exit 1
