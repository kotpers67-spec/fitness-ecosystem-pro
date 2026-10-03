Add-Type -AssemblyName System.IO.Compression.FileSystem
$tempDir = Join-Path $env:TEMP "apk_audit"
if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }
New-Item -ItemType Directory -Path (Join-Path $tempDir "desktop") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tempDir "release") -Force | Out-Null

[System.IO.Compression.ZipFile]::ExtractToDirectory("C:\Users\kotpe\OneDrive\Desktop\Fitness-Ecosystem-v1.0.5\athlete-pro-v1.0.5.apk", (Join-Path $tempDir "desktop"))
[System.IO.Compression.ZipFile]::ExtractToDirectory("F:\Projects\fitness-ecosystem-pro\releases\athlete-pro-v1.0.5.apk", (Join-Path $tempDir "release"))

Write-Host "=== Desktop athlete APK classes.dex check ==="
Get-ChildItem (Join-Path $tempDir "desktop\classes*.dex") | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    $text = [System.Text.Encoding]::UTF8.GetString($bytes)
    if ($text.Contains("QrCodeView")) { Write-Host "FOUND QrCodeView in desktop APK" }
    if ($text.Contains("fitnessapp.pro/pair")) { Write-Host "FOUND pairing url in desktop APK" }
    if ($text.Contains("isPrivateLeaderboard")) { Write-Host "FOUND isPrivateLeaderboard in desktop APK" }
}

Write-Host "=== Releases athlete APK classes.dex check ==="
Get-ChildItem (Join-Path $tempDir "release\classes*.dex") | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    $text = [System.Text.Encoding]::UTF8.GetString($bytes)
    if ($text.Contains("QrCodeView")) { Write-Host "FOUND QrCodeView in release APK" }
    if ($text.Contains("fitnessapp.pro/pair")) { Write-Host "FOUND pairing url in release APK" }
    if ($text.Contains("isPrivateLeaderboard")) { Write-Host "FOUND isPrivateLeaderboard in release APK" }
}

Remove-Item -Recurse -Force $tempDir
