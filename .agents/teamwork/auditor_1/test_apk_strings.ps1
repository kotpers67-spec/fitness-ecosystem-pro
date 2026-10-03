Add-Type -AssemblyName System.IO.Compression.FileSystem

# Gromov base64: '0JzQsNC60YHQuNC8INCT0YDQvtC80L7Qsg=='
$b64Gromov = "0JzQsNC60YHQuNC8INCT0YDQvtC80L7Qsg=="
$targetGromov = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($b64Gromov))

# Empty state header base64: "В состязаниях пока нет участников"
# '0JIg0YHQvtGB0YLRj9C30LDQvdC40Y/RhSDQv9C+0LrQsCDQvdC10YIg0YPRh9Cw0YHRgtC90LjQutC+0LI='
$b64Empty = "0JIg0YHQvtGB0YLRj9C30LDQvdC40Y/RhSDQv9C+0LrQsCDQvdC10YIg0YPRh9Cw0YHRgtC90LjQutC+0LI="
$targetEmpty = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($b64Empty))

function Check-ApkStrings([string]$apkPath, [string]$label) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($apkPath)
    $hasGromov = $false
    $hasEmpty = $false
    $hasPairingUrl = $false

    foreach ($entry in $zip.Entries) {
        if ($entry.Name.EndsWith(".dex")) {
            $stream = $entry.Open()
            $ms = New-Object System.IO.MemoryStream
            $stream.CopyTo($ms)
            $bytes = $ms.ToArray()
            $stream.Dispose()
            $ms.Dispose()

            $str = [System.Text.Encoding]::UTF8.GetString($bytes)
            if ($str.Contains($targetGromov)) { $hasGromov = $true }
            if ($str.Contains($targetEmpty)) { $hasEmpty = $true }
            if ($str.Contains("fitnessapp.pro/pair")) { $hasPairingUrl = $true }
        }
    }
    $zip.Dispose()
    Write-Host "$label : Gromov=$hasGromov, EmptyState=$hasEmpty, PairingUrl=$hasPairingUrl"
}

Check-ApkStrings "C:\Users\kotpe\OneDrive\Desktop\Fitness-Ecosystem-v1.0.5\athlete-pro-v1.0.5.apk" "DESKTOP"
Check-ApkStrings "F:\Projects\fitness-ecosystem-pro\releases\athlete-pro-v1.0.5.apk" "RELEASES_FOLDER"
