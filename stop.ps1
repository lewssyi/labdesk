$ErrorActionPreference = 'Stop'
$labListener = Get-NetTCPConnection -LocalPort 18080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if (!$labListener) { Write-Host 'LabDesk is not running.'; return }
$labProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($labListener.OwningProcess)"
if (!$labProcess.CommandLine -or $labProcess.CommandLine.Replace('/', '\') -notlike '*E:\Java\labdesk\target\labdesk-0.1.0.jar*') {
    throw 'Port 18080 belongs to another process. Nothing was stopped.'
}
Stop-Process -Id $labListener.OwningProcess
Write-Host 'LabDesk stopped. Shared MySQL and Redis are still available.'
