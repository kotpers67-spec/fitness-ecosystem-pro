Add-Type -AssemblyName System.IO.Compression.FileSystem

$apkFiles = @(
    "F:\Projects\fitness-ecosystem-pro\releases\trainer-pro-v1.0.5.apk",
    "F:\Projects\fitness-ecosystem-pro\releases\athlete-pro-v1.0.5.apk"
)

$banned = @('Громов', 'Соколова', 'Воронов', 'Морозова')
$foundViolations = 0

foreach ($apk in $apkFiles) {
    Write-Host "Checking $apk..."
    $zip = [System.IO.Compression.ZipFile]::OpenRead($apk)
    $dexEntries = $zip.Entries | Where-Object { $_.Name.EndsWith('.dex') }
    foreach ($entry in $dexEntries) {
        $stream = $entry.Open()
        $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
        $content = $reader.ReadToEnd()
        $stream.Close()
        foreach ($name in $banned) {
            if ($content.Contains($name)) {
                Write-Host "VIOLATION: Found $name in $($entry.Name) of $apk"
                $foundViolations++
            }
        }
    }
    $zip.Dispose()
    Write-Host "Completed scan for $apk"
}

Write-Host "Total mock violations found in DEX: $foundViolations"
if ($foundViolations -eq 0) {
    Write-Host "DEX MOCK SCAN: CLEAN"
} else {
    Write-Host "DEX MOCK SCAN: FAILED"
}
