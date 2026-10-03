$adb = "F:\Development\Android\Sdk\platform-tools\adb.exe"
$outDir = "C:\Users\kotpe\.gemini\antigravity\brain\c3e18e84-ccc9-4f18-a98a-305f3920010f\qa_screenshots"

& $adb shell input tap 1000 210
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_settings_final.png
& $adb pull /sdcard/s_settings_final.png "$outDir\15_settings_success.png"
