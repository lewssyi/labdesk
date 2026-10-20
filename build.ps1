$ErrorActionPreference = 'Stop'
$labRunning = Get-NetTCPConnection -LocalPort 18080 -State Listen -ErrorAction SilentlyContinue
if ($labRunning) { throw 'Stop LabDesk before rebuilding; Windows locks the running JAR.' }
$env:JAVA_HOME = 'E:\Java\_tools\jdk-21.0.12.1+1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Push-Location $PSScriptRoot
try {
    $labMavenArgs = @('-B', '-ntp', 'clean', 'package')
    if (Get-NetTCPConnection -LocalPort 7897 -State Listen -ErrorAction SilentlyContinue) {
        $labMavenArgs = @('-s', 'dev/maven-settings.xml') + $labMavenArgs
    }
    & 'D:\Program Files\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd' @labMavenArgs
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
} finally { Pop-Location }
