$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Path build -Force | Out-Null
javac -encoding UTF-8 -d build server/src/servicedesk/Main.java
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
Write-Host 'Open http://localhost:8080 in your browser.'
java -cp build servicedesk.Main 8080
