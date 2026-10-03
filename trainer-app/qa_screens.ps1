$adb = "F:\Development\Android\Sdk\platform-tools\adb.exe"
$outDir = "C:\Users\kotpe\.gemini\antigravity\brain\c3e18e84-ccc9-4f18-a98a-305f3920010f\qa_screenshots"

Write-Host "[QA] 1. Tapping Settings gear (930, 200)..."
& $adb shell input tap 930 200
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_settings.png
& $adb pull /sdcard/s_settings.png "$outDir\02_settings_live.png"

Write-Host "[QA] 2. Back to Home..."
& $adb shell input tap 70 200
Start-Sleep -Seconds 1

Write-Host "[QA] 3. Tapping Workout (540, 1800)..."
& $adb shell input tap 540 1800
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_workout.png
& $adb pull /sdcard/s_workout.png "$outDir\03_workout_live.png"

Write-Host "[QA] 4. Adding an exercise to workout (+ Упр. button)..."
& $adb shell input tap 540 1350
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_add_dialog.png
& $adb pull /sdcard/s_add_dialog.png "$outDir\04_add_dialog_live.png"

Write-Host "[QA] 5. Selecting exercise (Bench Press)..."
& $adb shell input tap 540 1300
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_workout_with_sets.png
& $adb pull /sdcard/s_workout_with_sets.png "$outDir\05_workout_sets_live.png"

Write-Host "[QA] 6. Tapping Back to Home..."
& $adb shell input tap 70 200
Start-Sleep -Seconds 1

Write-Host "[QA] 7. Tapping History (540, 1950)..."
& $adb shell input tap 540 1950
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s_history.png
& $adb pull /sdcard/s_history.png "$outDir\06_history_live.png"
