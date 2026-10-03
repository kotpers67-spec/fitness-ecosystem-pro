$keyStr = "Spirit5449@2011@213@"
$sha256 = [System.Security.Cryptography.SHA256]::Create()
$keyBytes = $sha256.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($keyStr))

$endpoint = "https://script.google.com/macros/s/AKfycbx6LCbVlxZa-MsWrP0QlNouJwcEcZVsbYHlO4HwHPDDuT6_dp0FDUKTmU_Ax5vg7EP6/exec"
$encodedKey = [System.Web.HttpUtility]::UrlEncode($keyStr)
if (-not $encodedKey) {
    $encodedKey = [System.Uri]::EscapeDataString($keyStr)
}
$url = "$endpoint`?key=$encodedKey"

Write-Host "Fetching cloud data from: $url"
$response = Invoke-RestMethod -Uri $url -Method Get -MaximumRedirection 5
Write-Host "Raw response length: $($response.Length)"

if ($response.StartsWith("ENC:")) {
    $cipherBytes = [System.Convert]::FromBase64String($response.Substring(4))
    $aes = [System.Security.Cryptography.Aes]::Create()
    $aes.Key = $keyBytes
    $aes.Mode = [System.Security.Cryptography.CipherMode]::ECB
    $aes.Padding = [System.Security.Cryptography.PaddingMode]::PKCS7
    $decryptor = $aes.CreateDecryptor()
    $plainBytes = $decryptor.TransformFinalBlock($cipherBytes, 0, $cipherBytes.Length)
    $plainText = [System.Text.Encoding]::UTF8.GetString($plainBytes)
    Write-Host "Decrypted JSON:"
    Write-Host $plainText
} else {
    Write-Host "Unencrypted response: $response"
}
