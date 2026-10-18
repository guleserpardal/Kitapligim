# Yönetici olarak çalıştır:  powershell -ExecutionPolicy Bypass -File .\admin-kur.ps1
# Güvenlik duvarı kuralını (8790, yalnız yerel ağ + Tailscale) ve oturum açılınca başlayan zamanlanmış görevi kurar.
# Spor Hocam (8789) ve İlker'in kurulumuna (8788) dokunmaz.
$ErrorActionPreference = 'Stop'
$port = 8790
$ruleName = 'Kitapligim Sunucu'
$taskName = 'Kitapligim Sunucu'
$serverDir = $PSScriptRoot

if (-not (Test-Path (Join-Path $serverDir '.env'))) { throw ".env yok. .env.example dosyasını .env olarak kopyalayıp APP_TOKEN değerini yaz." }
$node = (Get-Command node -ErrorAction Stop).Source

Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue | Remove-NetFirewallRule
New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Action Allow -Protocol TCP -LocalPort $port `
  -RemoteAddress @('LocalSubnet', '100.64.0.0/10') -Profile Any | Out-Null

$action = New-ScheduledTaskAction -Execute $node -Argument 'src\server.js' -WorkingDirectory $serverDir
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet -RestartCount 99 -RestartInterval (New-TimeSpan -Minutes 1) -StartWhenAvailable -ExecutionTimeLimit ([TimeSpan]::Zero) -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -RunLevel Limited | Out-Null
Start-ScheduledTask -TaskName $taskName

Write-Host "Tamam. Sağlık kontrolü: http://localhost:$port/api/health"
