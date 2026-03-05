param(
  [string]$Distro = "Ubuntu"
)

$ErrorActionPreference = "Stop"

# repo root = scripts 폴더의 상위
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

# Windows 경로를 WSL 경로로 변환
$WinRepoRoot = $RepoRoot.Path
$Drive = [string]::Empty
$WslPath = [string]::Empty

if ($WinRepoRoot.Length -ge 2 -and $WinRepoRoot[1] -eq ':') {
    $Drive = $WinRepoRoot.Substring(0, 1).ToLower()
    $Rest = $WinRepoRoot.Substring(2).TrimStart('\')
    $WslPath = "/mnt/$Drive/$($Rest -replace '\\', '/')"
} else {
    Write-Error "Unable to convert Windows path to WSL path: $WinRepoRoot"
}

# 실행
wsl.exe -d $Distro -- bash -lc "cd '$WslPath' && bash scripts/swarm-sprint.sh"
