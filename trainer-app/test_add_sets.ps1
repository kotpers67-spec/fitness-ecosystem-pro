$adb = "F:\Development\Android\Sdk\platform-tools\adb.exe"
$outDir = "C:\Users\kotpe\.gemini\antigravity\brain\c3e18e84-ccc9-4f18-a98a-305f3920010f\qa_screenshots"

Write-Host "[QA] 1. Closing dialog by tapping close (730, 1550)..."
& $adb shell input tap 730 1550
Start-Sleep -Seconds 1

Write-Host "[QA] 2. Tapping add exercise button (500, 1680)..."
& $adb shell input tap 500 1680
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_dialog_open.png
& $adb pull /sdcard/s_dialog_open.png "$outDir\08_dialog_exercise_list.png"

Write-Host "[QA] 3. Tapping first exercise in list (500, 1150)..."
& $adb shell input tap 500 1150
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_workout_sets.png
& $adb pull /sdcard/s_workout_sets.png "$outDir\09_workout_sets_rendered.png"

Write-Host "[QA] 4. Tapping checkbox to complete set (960, 780)..."
& $adb shell input tap 960 780
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_timer_overlay.png
& $adb pull /sdcard/s_timer_overlay.png "$outDir\10_timer_overlay_active.png"
