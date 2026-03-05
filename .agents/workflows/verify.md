---
description: Gradle 빌드 검증 (clean + lint + test + assembleDebug)
---

# 검증

## Windows (PowerShell)
```powershell
.\scripts\verify.ps1
```

## Unix (Bash)
```bash
bash scripts/verify.sh
```

## 직접 Gradle 실행
```powershell
.\gradlew.bat --no-daemon clean lint test assembleDebug
```
