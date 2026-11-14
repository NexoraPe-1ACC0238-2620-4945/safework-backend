[CmdletBinding()]
param([int]$Port=18086,[string]$JavaHome='C:\Program Files\Android\Android Studio\jbr',[string]$TestRuntimeRoot)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if(-not $TestRuntimeRoot){$TestRuntimeRoot=Join-Path $repo '.local'}
$c=Import-Clixml -LiteralPath (Join-Path $TestRuntimeRoot 'test-mysql.clixml')
function Plain([Security.SecureString]$v){(New-Object Management.Automation.PSCredential('local',$v)).GetNetworkCredential().Password}
foreach($entry in Get-ChildItem Env:){if($entry.Name -match '^(DB_|SPRING_|BOOTSTRAP_|SAFEWORK_|JWT_|JAVA_TOOL_OPTIONS|JDK_JAVA_OPTIONS|_JAVA_OPTIONS)'){Remove-Item -LiteralPath ('Env:'+$entry.Name)}}
$env:DB_HOST='127.0.0.1';$env:DB_PORT='33319';$env:DB_NAME='safework_alternative_test';$env:DB_USERNAME='safework_alt'
$env:DB_PASSWORD=Plain $c.DB_PASSWORD;$env:JWT_SECRET=Plain $c.JWT_SECRET
$env:APP_PROFILE='dev';$env:DB_TLS='false';$env:DB_ALLOW_PUBLIC_KEY='true';$env:DDL_AUTO='validate';$env:MIGRATIONS_ENABLED='false'
$env:SERVER_ADDRESS='127.0.0.1';$env:SERVER_PORT=[string]$Port;$env:TZ='UTC'
$jar=Join-Path $repo 'target/service-1.0.0-SNAPSHOT.jar';$log=Join-Path $repo '.local/server.log'
Set-Location $repo
$ErrorActionPreference='Continue'
try{& (Join-Path $JavaHome 'bin/java.exe') '-Duser.timezone=UTC' '-jar' $jar 2>&1 | ForEach-Object{
    $line=$_.ToString();foreach($v in @($env:DB_PASSWORD,$env:JWT_SECRET)){$line=$line.Replace($v,'[REDACTED]')}
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
};exit $LASTEXITCODE}finally{$env:DB_PASSWORD=$null;$env:JWT_SECRET=$null}
