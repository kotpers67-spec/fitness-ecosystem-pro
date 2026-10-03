$keyStr = "Spirit5449@2011@213@"
$sha256 = [System.Security.Cryptography.SHA256]::Create()
$keyBytes = $sha256.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($keyStr))

$endpoint = "https://script.google.com/macros/s/AKfycbx6LCbVlxZa-MsWrP0QlNouJwcEcZVsbYHlO4HwHPDDuT6_dp0FDUKTmU_Ax5vg7EP6/exec"
$encodedKey = [System.Uri]::EscapeDataString($keyStr)
$url = "$endpoint`?key=$encodedKey"

Write-Host "1. Fetching current cloud data..."
$response = Invoke-RestMethod -Uri $url -Method Get -MaximumRedirection 5

if (-not $response.StartsWith("ENC:")) {
    Write-Error "Response is not ENC: $response"
    exit 1
}

# Decrypt
$cipherBytes = [System.Convert]::FromBase64String($response.Substring(4))
$aes = [System.Security.Cryptography.Aes]::Create()
$aes.Key = $keyBytes
$aes.Mode = [System.Security.Cryptography.CipherMode]::ECB
$aes.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
$decryptor = $aes.CreateDecryptor()
$plainBytes = $decryptor.TransformFinalBlock($cipherBytes, 0, $cipherBytes.Length)
$plainText = [System.Text.Encoding]::UTF8.GetString($plainBytes)

# Parse JSON
$data = $plainText | ConvertFrom-Json

Write-Host "Current cloud athleteVersion: $($data.updates.athleteVersion)"
Write-Host "Current cloud trainerVersion: $($data.updates.trainerVersion)"

# Update to 1.0.6
$data.updates.athleteVersion = "1.0.6"
$data.updates.athleteUrl = "https://github.com/santiyastudio-lgtm/fitness-ecosystem-pro/releases/download/v1.0.6/athlete-pro-v1.0.6.apk"
$data.updates.trainerVersion = "1.0.6"
$data.updates.trainerUrl = "https://github.com/santiyastudio-lgtm/fitness-ecosystem-pro/releases/download/v1.0.6/trainer-pro-v1.0.6.apk"
$data.updates.notes = "Версия 1.0.6: Вход и регистрация, очистка состязаний, удаление лишних кнопок у атлета, стабильное обновление."
$data.updatedAt = [string][DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

$newJson = $data | ConvertTo-Json -Depth 20 -Compress

# Encrypt
$newPlainBytes = [System.Text.Encoding]::UTF8.GetBytes($newJson)
$encryptor = $aes.CreateEncryptor()
$newCipherBytes = $encryptor.TransformFinalBlock($newPlainBytes, 0, $newPlainBytes.Length)
$newEncString = "ENC:" + [System.Convert]::ToBase64String($newCipherBytes)

Write-Host "2. Uploading updated version 1.0.6 to cloud..."
$postRes = Invoke-RestMethod -Uri $url -Method Post -Body $newEncString -ContentType "application/json" -MaximumRedirection 5
Write-Host "POST Result: $postRes"

Write-Host "3. Verifying updated cloud node..."
Start-Sleep -Seconds 1
$verifyRes = Invoke-RestMethod -Uri $url -Method Get -MaximumRedirection 5
$vBytes = [System.Convert]::FromBase64String($verifyRes.Substring(4))
$vPlain = [System.Text.Encoding]::UTF8.GetString($decryptor.TransformFinalBlock($vBytes, 0, $vBytes.Length))
$vData = $vPlain | ConvertFrom-Json

Write-Host "Verified Athlete Version in Cloud: $($vData.updates.athleteVersion)"
Write-Host "Verified Trainer Version in Cloud: $($vData.updates.trainerVersion)"
