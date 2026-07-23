# Automated API integration verification script for Milestone 5

Write-Output "=== Starting API Integration Tests ==="

$Port = 8080
$Url = "http://localhost:$Port"

# 1. Start Spring Boot App in background
Write-Output "Starting Spring Boot application jar..."
$Process = Start-Process java -ArgumentList "-Dspring.profiles.active=mysql -jar target/securecrypt-platform-1.0-SNAPSHOT.jar" -PassThru -NoNewWindow

# 2. Wait for Spring Boot to initialize
Write-Output "Waiting for port $Port to open (approx 12 seconds)..."
for ($i = 1; $i -le 12; $i++) {
    Start-Sleep -Seconds 1
    if (Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue) {
        Write-Output "Spring Boot server initialized on port $Port!"
        break
    }
}

if (-not (Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue)) {
    Write-Error "Spring Boot server failed to initialize in time."
    Stop-Process -Id $Process.Id -Force
    exit 1
}

$TestSuccess = $true

try {
    # 3. Test Registration Endpoint
    Write-Output "`n[TEST] Registering user 'cryptodev'..."
    $RegBody = @{
        username = "cryptodev"
        password = "DevPassword@123"
    } | ConvertTo-Json

    $RegResponse = Invoke-RestMethod -Uri "$Url/api/auth/register" -Method Post -ContentType "application/json" -Body $RegBody
    Write-Output "Registration Response: $($RegResponse | ConvertTo-Json -Compress)"

    # 4. Test Login Endpoint
    Write-Output "`n[TEST] Logging in..."
    $LoginResponse = Invoke-RestMethod -Uri "$Url/api/auth/login" -Method Post -ContentType "application/json" -Body $RegBody
    Write-Output "Login Response (token obtained): $($LoginResponse.token.Substring(0, 15))..."
    $Token = $LoginResponse.token

    # 5. Test Metadata Upload Endpoint
    Write-Output "`n[TEST] Uploading zero-knowledge metadata..."
    $MetadataBody = @{
        originalFilename = "my_private_keys.txt"
        encryptedFilename = "e9df28c0-87a4-4d1e-8e81-7abcefe9b21f.scf"
        fileSize = 40960
        algorithm = "AES-256-GCM"
    } | ConvertTo-Json

    $Headers = @{
        Authorization = "Bearer $Token"
    }

    $UploadResponse = Invoke-RestMethod -Uri "$Url/api/files/upload-metadata" -Method Post -ContentType "application/json" -Headers $Headers -Body $MetadataBody
    Write-Output "Upload Response: $($UploadResponse | ConvertTo-Json -Compress)"

    # 6. Test Metadata Listing Endpoint
    Write-Output "`n[TEST] Listing user metadata..."
    $ListResponse = Invoke-RestMethod -Uri "$Url/api/files/list-metadata" -Method Get -Headers $Headers
    Write-Output "Listing Response: $($ListResponse | ConvertTo-Json)"

    # 7. Assertions
    $Matched = $false
    foreach ($item in $ListResponse) {
        if ($item.originalFilename -eq "my_private_keys.txt" -and $item.fileSize -eq 40960) {
            $Matched = $true
            break
        }
    }

    if ($Matched) {
        Write-Output "`n[SUCCESS] REST API Verification: PASSED!"
    } else {
        Write-Error "`n[FAILURE] REST API Verification: FAILED (Metadata mismatch)."
        $TestSuccess = $false
    }

} catch {
    Write-Error "Exception occurred during REST validation: $_"
    $TestSuccess = $false
} finally {
    # 8. Clean Shutdown of Spring Boot
    Write-Output "`nStopping Spring Boot server process (PID $($Process.Id))..."
    Stop-Process -Id $Process.Id -Force
    Write-Output "Process stopped."
}

if ($TestSuccess) {
    exit 0
} else {
    exit 1
}
