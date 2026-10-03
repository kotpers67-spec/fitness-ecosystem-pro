$xmls = Get-ChildItem -Path "F:\Projects\fitness-ecosystem-pro\athlete-app\app\build\test-results\testDebugUnitTest" -Filter "*.xml" -ErrorAction SilentlyContinue
$totalTests = 0
$totalFailures = 0
$totalSkipped = 0
if ($xmls) {
    foreach ($xml in $xmls) {
        [xml]$x = Get-Content $xml.FullName
        $totalTests += [int]$x.testsuite.tests
        $totalFailures += [int]$x.testsuite.failures
        $totalSkipped += [int]$x.testsuite.skipped
    }
}
Write-Host "Athlete Tests: Total=$totalTests, Failures=$totalFailures, Skipped=$totalSkipped"

$xmlsTrainer = Get-ChildItem -Path "F:\Projects\fitness-ecosystem-pro\trainer-app\app\build\test-results\testDebugUnitTest" -Filter "*.xml" -ErrorAction SilentlyContinue
$totalTestsTrainer = 0
$totalFailuresTrainer = 0
$totalSkippedTrainer = 0
if ($xmlsTrainer) {
    foreach ($xml in $xmlsTrainer) {
        [xml]$x = Get-Content $xml.FullName
        $totalTestsTrainer += [int]$x.testsuite.tests
        $totalFailuresTrainer += [int]$x.testsuite.failures
        $totalSkippedTrainer += [int]$x.testsuite.skipped
    }
}
Write-Host "Trainer Tests: Total=$totalTestsTrainer, Failures=$totalFailuresTrainer, Skipped=$totalSkippedTrainer"
