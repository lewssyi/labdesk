param([switch]$Rebuild)
$ErrorActionPreference = 'Stop'
$labLogs = 'E:\Java\_runtime\logs'
New-Item -ItemType Directory -Force -Path $labLogs | Out-Null

function Start-LabService($Name, $Port, $File, $Arguments, $Directory, $Marker) {
    $labListener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($labListener) {
        $labExisting = Get-CimInstance Win32_Process -Filter "ProcessId=$($labListener.OwningProcess)"
        if (!$labExisting.CommandLine -or $labExisting.CommandLine.Replace('/', '\') -notlike ('*' + $Marker.Replace('/', '\') + '*')) {
            throw "Port $Port is occupied by another application."
        }
        Write-Host "$Name already running on port $Port"
        return
    }
    $labStarted = Start-Process -FilePath $File -ArgumentList $Arguments -WorkingDirectory $Directory -WindowStyle Hidden -RedirectStandardOutput (Join-Path $labLogs "$Name-stdout.log") -RedirectStandardError (Join-Path $labLogs "$Name-stderr.log") -PassThru
    for ($labAttempt = 0; $labAttempt -lt 45; $labAttempt++) {
        if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
            Write-Host "$Name ready on port $Port"
            return
        }
        if ($labStarted.HasExited) { throw "$Name exited. See $labLogs" }
        Start-Sleep -Seconds 1
    }
    throw "$Name is not ready yet. See $labLogs"
}

# 复用本机已有的隔离开发实例；无需运行 yudao 后端或前端。
Start-LabService 'mysql' 3307 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe' '--defaults-file=E:\Java\_runtime\mysql\my.ini' 'E:\Java' 'E:\Java\_runtime\mysql\my.ini'
Start-LabService 'redis' 6379 'E:\Java\_tools\redis\Redis-8.10.2-Windows-x64-cygwin\redis-server.exe' '/cygdrive/e/Java/_runtime/redis/redis.conf' 'E:\Java\_tools\redis\Redis-8.10.2-Windows-x64-cygwin' '/cygdrive/e/Java/_runtime/redis/redis.conf'
$labJar = Join-Path $PSScriptRoot 'target\labdesk-0.1.0.jar'
if ($Rebuild -or !(Test-Path -LiteralPath $labJar)) { & (Join-Path $PSScriptRoot 'build.ps1') }
Start-LabService 'labdesk' 18080 'E:\Java\_tools\jdk-21.0.12.1+1\bin\java.exe' '-Xms128m -Xmx512m -Dfile.encoding=UTF-8 -jar E:\Java\labdesk\target\labdesk-0.1.0.jar' $PSScriptRoot 'E:\Java\labdesk\target\labdesk-0.1.0.jar'
Write-Host 'Open http://127.0.0.1:18080/  student / student123  admin / admin123'
