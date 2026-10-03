$keyStr = "Spirit5449@2011@213@"
$sha256 = [System.Security.Cryptography.SHA256]::Create()
$keyBytes = $sha256.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($keyStr))

$endpoint = "https://script.google.com/macros/s/AKfycbx6LCbVlxZa-MsWrP0QlNouJwcEcZVsbYHlO4HwHPDDuT6_dp0FDUKTmU_Ax5vg7EP6/exec"
$encodedKey = [System.Uri]::EscapeDataString($keyStr)
$url = $endpoint + "?key=" + $encodedKey

Write-Host "Fetching cloud data..."
$response = Invoke-RestMethod -Uri $url -Method Get -MaximumRedirection 5
$cipherBytes = [System.Convert]::FromBase64String($response.Substring(4))
$aes = [System.Security.Cryptography.Aes]::Create()
$aes.Key = $keyBytes
$aes.Mode = [System.Security.Cryptography.CipherMode]::ECB
$aes.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
$decryptor = $aes.CreateDecryptor()
$plainBytes = $decryptor.TransformFinalBlock($cipherBytes, 0, $cipherBytes.Length)
$plainText = [System.Text.Encoding]::UTF8.GetString($plainBytes)
$data = $plainText | ConvertFrom-Json

# Clear clients and pairing
$data.clients = [PSCustomObject]@{}
$data.pairing = [PSCustomObject]@{}
$data.updatedAt = [string][DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

$newJson = $data | ConvertTo-Json -Depth 20 -Compress
$newPlainBytes = [System.Text.Encoding]::UTF8.GetBytes($newJson)
$encryptor = $aes.CreateEncryptor()
$newCipherBytes = $encryptor.TransformFinalBlock($newPlainBytes, 0, $newPlainBytes.Length)
$newEncString = "ENC:" + [System.Convert]::ToBase64String($newCipherBytes)

Write-Host "Uploading cleared cloud state..."
$postRes = Invoke-RestMethod -Uri $url -Method Post -Body $newEncString -ContentType "application/json" -MaximumRedirection 5
Write-Host "Cloud cleared! Result: $($postRes | ConvertTo-Json)"
