# Fitness Ecosystem Pro — Web Server Launcher (PowerShell)
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$NodeExe = "node"
$FoundNode = Get-Command node -ErrorAction SilentlyContinue

if (-not $FoundNode) {
    if (Test-Path "C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe") {
        $NodeExe = "C:\Users\kotpe\AppData\Local\OpenAI\Codex\runtimes\cua_node\45309f9050f7314b\bin\node.exe"
    } elseif (Test-Path "C:\Users\kotpe\AppData\Local\Programs\antigravity\Antigravity.exe") {
        $env:ELECTRON_RUN_AS_NODE = "1"
        $NodeExe = "C:\Users\kotpe\AppData\Local\Programs\antigravity\Antigravity.exe"
    } else {
        Write-Error "Node.js executable could not be found."
        exit 1
    }
}

Write-Host "[Fitness Ecosystem Web] Starting server on http://localhost:3000..." -ForegroundColor Green
& $NodeExe src/server.js
