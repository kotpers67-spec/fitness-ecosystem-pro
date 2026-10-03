$adb = "F:\Development\Android\Sdk\platform-tools\adb.exe"
$outDir = "C:\Users\kotpe\.gemini\antigravity\brain\c3e18e84-ccc9-4f18-a98a-305f3920010f\qa_screenshots"

Write-Host "[QA] Unlocking screen..."
& $adb shell input keyevent 224
& $adb shell wm dismiss-keyguard
& $adb shell input keyevent 82
Start-Sleep -Seconds 1

Write-Host "[QA] Launching Trainer Pro App..."
& $adb shell am start -n com.trainerapp.pro/.MainActivity
Start-Sleep -Seconds 3

Write-Host "[QA] Capturing 01_home_screen.png..."
& $adb shell screencap -p /sdcard/s1.png
& $adb pull /sdcard/s1.png "$outDir\01_home_screen.png"

Write-Host "[QA] Opening Settings..."
& $adb shell input tap 990 140
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s2.png
& $adb pull /sdcard/s2.png "$outDir\02_settings_screen.png"

Write-Host "[QA] Back to Home..."
& $adb shell input tap 70 140
Start-Sleep -Seconds 1

Write-Host "[QA] Tapping Workout Button (middle-bottom: 540, 1900)..."
& $adb shell input tap 540 1900
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s3.png
& $adb pull /sdcard/s3.png "$outDir\03_workout_screen.png"

Write-Host "[QA] Adding Exercise (+ Упр.)..."
& $adb shell input tap 990 470
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s4.png
& $adb pull /sdcard/s4.png "$outDir\04_add_exercise_dialog.png"

Write-Host "[QA] Selecting first exercise from dialog (540, 1000)..."
& $adb shell input tap 540 1000
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s5.png
& $adb pull /sdcard/s5.png "$outDir\05_workout_exercise_active.png"

Write-Host "[QA] Tapping Checkbox to complete Set 1 (trigger Rest Timer: 990, 830)..."
& $adb shell input tap 990 830
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s6.png
& $adb pull /sdcard/s6.png "$outDir\06_workout_timer_active.png"

Write-Host "[QA] Back to Home & Opening History (540, 2050)..."
& $adb shell input tap 70 140
Start-Sleep -Seconds 1
& $adb shell input tap 540 2050
Start-Sleep -Seconds 2
& $adb shell screencap -p /sdcard/s7.png
& $adb pull /sdcard/s7.png "$outDir\07_history_screen.png"

Write-Host "[QA] Checking logcat for errors..."
$errors = & $adb logcat -d *:E | Select-String -Pattern "FATAL|AndroidRuntime"
if ($errors) {
    Write-Host "[QA WARNING] Logcat errors:"
    $errors | Select-Object -First 5
} else {
    Write-Host "[QA SUCCESS] ZERO fatal crashes or runtime errors in Logcat!"
}
