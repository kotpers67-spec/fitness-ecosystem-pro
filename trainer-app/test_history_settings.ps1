$adb = "F:\Development\Android\Sdk\platform-tools\adb.exe"
$outDir = "C:\Users\kotpe\.gemini\antigravity\brain\c3e18e84-ccc9-4f18-a98a-305f3920010f\qa_screenshots"

Write-Host "[QA] 1. Tapping Back to Home (70, 200)..."
& $adb shell input tap 70 200
Start-Sleep -Seconds 1

Write-Host "[QA] 2. Tapping HISTORY AND CHARTS (540, 2150)..."
& $adb shell input tap 540 2150
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_history_full.png
& $adb pull /sdcard/s_history_full.png "$outDir\11_history_screen_full.png"

Write-Host "[QA] 3. Tapping Back to Home (70, 200)..."
& $adb shell input tap 70 200
Start-Sleep -Seconds 1

Write-Host "[QA] 4. Tapping Settings Gear (930, 200)..."
& $adb shell input tap 930 200
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_settings_full.png
& $adb pull /sdcard/s_settings_full.png "$outDir\12_settings_screen_full.png"
