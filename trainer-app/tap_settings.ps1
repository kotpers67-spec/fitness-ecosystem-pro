$adb = "F:\Development\Android\Sdk\platform-tools\adb.exe"
$outDir = "C:\Users\kotpe\.gemini\antigravity\brain\c3e18e84-ccc9-4f18-a98a-305f3920010f\qa_screenshots"

& $adb shell input tap 930 220
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_settings_real.png
& $adb pull /sdcard/s_settings_real.png "$outDir\13_settings_real.png"
