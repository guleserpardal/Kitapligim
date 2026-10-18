# Yonetici olarak calistir:  powershell -ExecutionPolicy Bypass -File .\admin-kur.ps1
# Yapar: .env yoksa rastgele APP_TOKEN uretip olusturur, guvenlik duvari kuralini (8790, yalniz yerel ag + Tailscale)
# ve oturum acilinca baslayan zamanlanmis gorevi kurar, saglik kontrolu yapip telefona girilecek bilgileri yazar.
# Spor Hocam (8789) ve Ilker'in kurulumuna (8788) dokunmaz. Tekrar calistirmak guvenlidir; mevcut .env'e dokunmaz.
# NOT: Bu dosya bilerek yalniz ASCII karakter icerir (Windows PowerShell 5.1 BOM'suz UTF-8'i yanlis okur).
$ErrorActionPreference = 'Stop'
$port = 8790
$ruleName = 'Kitapligim Sunucu'
$taskName = 'Kitapligim Sunucu'
$serverDir = $PSScriptRoot
$envFile = Join-Path $serverDir '.env'

$node = (Get-Command node -ErrorAction Stop).Source
if (-not (Get-Command claude -ErrorAction SilentlyContinue)) {
  Write-Warning "claude komutu bulunamadi. Claude Code kurulu ve giris yapilmis olmali; yoksa Danisman yanit veremez (.env icinde CLAUDE_BIN ile tam yol verilebilir)."
}

$newToken = $null
if (-not (Test-Path $envFile)) {
  $bytes = New-Object byte[] 24
  [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  $newToken = -join ($bytes | ForEach-Object { $_.ToString('x2') })
  $lines = @(
    "APP_TOKEN=$newToken",
    "PORT=$port",
    "HOST=0.0.0.0",
    "CLAUDE_BIN=claude",
    "CLAUDE_MODEL=sonnet",
    "CLAUDE_TIMEOUT_MS=120000",
    "ENRICH=1"
  )
  Set-Content -Path $envFile -Value $lines -Encoding ascii
  Write-Host ".env olusturuldu."
}
$tokenLine = Get-Content $envFile | Where-Object { $_ -match '^APP_TOKEN=.+' } | Select-Object -First 1
if (-not $tokenLine) { throw ".env icinde APP_TOKEN bos. Uzun, rastgele bir deger yaz ve betigi yeniden calistir." }

Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue | Remove-NetFirewallRule
New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Action Allow -Protocol TCP -LocalPort $port `
  -RemoteAddress @('LocalSubnet', '100.64.0.0/10') -Profile Any | Out-Null

$action = New-ScheduledTaskAction -Execute $node -Argument 'src\server.js' -WorkingDirectory $serverDir
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet -RestartCount 99 -RestartInterval (New-TimeSpan -Minutes 1) -StartWhenAvailable -ExecutionTimeLimit ([TimeSpan]::Zero) -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -RunLevel Limited | Out-Null
Start-ScheduledTask -TaskName $taskName
Start-Sleep -Seconds 3

try {
  $health = Invoke-RestMethod "http://localhost:$port/api/health" -TimeoutSec 10
  Write-Host "Sunucu ayakta: ok=$($health.ok) tokenSet=$($health.tokenSet)" -ForegroundColor Green
} catch {
  Write-Warning "Saglik kontrolu basarisiz: $($_.Exception.Message). Elle dene: node src\server.js"
}

$ips = Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } | Select-Object -ExpandProperty IPAddress
Write-Host ""
Write-Host "Telefonda Kitapligim > Ayarlar > Kitap Danismani bolumune gir:" -ForegroundColor Cyan
foreach ($ip in $ips) { Write-Host "  Adres: http://${ip}:$port" }
Write-Host "  (Uygulama 192.168.1.12 ve 100.68.128.112 disindaki adreslere sifresiz baglanamaz; farkliysa haber ver.)"
if ($newToken) { Write-Host "  Anahtar: $newToken" } else { Write-Host "  Anahtar: $($tokenLine -replace '^APP_TOKEN=','')  (mevcut .env)" }
