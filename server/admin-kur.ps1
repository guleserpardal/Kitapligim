# Yönetici olarak çalıştır:  powershell -ExecutionPolicy Bypass -File .\admin-kur.ps1
# Yapar: .env yoksa rastgele APP_TOKEN üretip oluşturur, güvenlik duvarı kuralını (8790, yalnız yerel ağ + Tailscale)
# ve oturum açılınca başlayan zamanlanmış görevi kurar, sağlık kontrolü yapıp telefona girilecek bilgileri yazar.
# Spor Hocam (8789) ve İlker'in kurulumuna (8788) dokunmaz. Tekrar çalıştırmak güvenlidir; mevcut .env'e dokunmaz.
$ErrorActionPreference = 'Stop'
$port = 8790
$ruleName = 'Kitapligim Sunucu'
$taskName = 'Kitapligim Sunucu'
$serverDir = $PSScriptRoot
$envFile = Join-Path $serverDir '.env'

$node = (Get-Command node -ErrorAction Stop).Source
if (-not (Get-Command claude -ErrorAction SilentlyContinue)) { Write-Warning "claude komutu bulunamadı. Claude Code kurulu ve giriş yapılmış olmalı; yoksa Danışman yanıt veremez (.env içinde CLAUDE_BIN ile tam yol verilebilir)." }

$newToken = $null
if (-not (Test-Path $envFile)) {
  $bytes = New-Object byte[] 24
  [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  $newToken = -join ($bytes | ForEach-Object { $_.ToString('x2') })
  (Get-Content (Join-Path $serverDir '.env.example')) -replace '^APP_TOKEN=.*$', "APP_TOKEN=$newToken" | Set-Content $envFile -Encoding ascii
  Write-Host ".env oluşturuldu."
}
$tokenLine = Get-Content $envFile | Where-Object { $_ -match '^APP_TOKEN=.+' }
if (-not $tokenLine) { throw ".env içinde APP_TOKEN boş. Uzun, rastgele bir değer yaz ve betiği yeniden çalıştır." }

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
  Write-Warning "Sağlık kontrolü başarısız: $($_.Exception.Message). Günlük için elle dene: node src\server.js"
}

$ips = Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } | Select-Object -ExpandProperty IPAddress
Write-Host ""
Write-Host "Telefonda Kitaplığım > Ayarlar > Kitap Danışmanı bölümüne gir:" -ForegroundColor Cyan
foreach ($ip in $ips) { Write-Host "  Adres: http://${ip}:$port" }
Write-Host "  (Uygulama 192.168.1.12 ve 100.68.128.112 dışındaki adreslere şifresiz bağlanamaz; farklıysa haber ver.)"
if ($newToken) { Write-Host "  Anahtar: $newToken" } else { Write-Host "  Anahtar: $((($tokenLine -replace '^APP_TOKEN=','')))  (mevcut .env)" }
