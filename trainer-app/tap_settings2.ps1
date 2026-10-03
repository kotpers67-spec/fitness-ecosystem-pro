$adb = "F:\Development\Android\Sdk\platform-tools\adb.exe"
$outDir = "C:\Users\kotpe\.gemini\antigravity\brain\c3e18e84-ccc9-4f18-a98a-305f3920010f\qa_screenshots"

& $adb shell input tap 930 90
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_settings_real2.png
& $adb pull /sdcard/s_settings_real2.png "$outDir\14_settings_view.png"
