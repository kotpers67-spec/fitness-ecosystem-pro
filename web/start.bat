@echo off
setlocal
cd /d "%~dp0"

set "NODE_EXE=node"
where node >nul 2>nul
if %errorlevel% neq 0 (
  if exist "C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe" (
    set "NODE_EXE=C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe"
  ) else (
    if exist "C:\Users\kotpe\AppData\Local\Programs\antigravity\Antigravity.exe" (
      set "ELECTRON_RUN_AS_NODE=1"
      set "NODE_EXE=C:\Users\kotpe\AppData\Local\Programs\antigravity\Antigravity.exe"
    )
  )
)

echo [Fitness Ecosystem Web] Starting server on http://localhost:3000...
"%NODE_EXE%" src/server.js
