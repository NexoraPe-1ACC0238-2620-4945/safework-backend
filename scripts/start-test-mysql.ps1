[CmdletBinding()]
param([string]$MySqlBin='C:\Program Files\MySQL\MySQL Server 9.5\bin')
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$runtime=Join-Path $repo '.local'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
$secretFile=Join-Path $runtime 'test-mysql.clixml'
if (Test-Path -LiteralPath $secretFile) {
    if(-not(Get-NetTCPConnection -State Listen -LocalPort 33319 -ErrorAction SilentlyContinue)) {
        $existingIni=Join-Path $runtime 'mysql.ini'
        if(-not(Test-Path -LiteralPath $existingIni)){throw 'Existing fixture config missing; no database was reinitialized.'}
        $p=Start-Process -FilePath (Join-Path $MySqlBin 'mysqld.exe') -ArgumentList ('--defaults-file="'+$existingIni+'"') -WindowStyle Hidden -PassThru
        $p.Id|Set-Content -LiteralPath (Join-Path $runtime 'mysql-host.pid')
        Write-Output 'EXISTING_ISOLATED_MYSQL_STARTING'
    }else{Write-Output 'TEST_MYSQL_ALREADY_PROVISIONED'}
    exit 0
}
if (Get-NetTCPConnection -State Listen -LocalPort 33319 -ErrorAction SilentlyContinue) { throw 'Port 33319 is occupied; no existing database will be changed.' }
$data=Join-Path $runtime 'mysql-data'
if (Test-Path -LiteralPath $data) { throw 'Data directory exists; review the earlier attempt rather than overwriting it.' }
New-Item -ItemType Directory -Path $data | Out-Null
$ini=Join-Path $runtime 'mysql.ini'
$dataForward=$data.Replace('\','/')
$logForward=(Join-Path $runtime 'mysql.log').Replace('\','/')
[IO.File]::WriteAllText($ini,"[mysqld]`nbasedir=C:/Program Files/MySQL/MySQL Server 9.5`ndatadir=$dataForward`nport=33319`nbind-address=127.0.0.1`nmysqlx=0`nskip-log-bin`ninnodb_buffer_pool_size=128M`nlog-error=$logForward`n")
& (Join-Path $mysqlBin 'mysqld.exe') ('--defaults-file='+$ini) --initialize-insecure
if ($LASTEXITCODE -ne 0) { throw 'Isolated MySQL initialization failed.' }
$p=Start-Process -FilePath (Join-Path $mysqlBin 'mysqld.exe') -ArgumentList ('--defaults-file="'+$ini+'"') -WindowStyle Hidden -PassThru
$p.Id | Set-Content -LiteralPath (Join-Path $runtime 'mysql-host.pid')
$ready=$false
for($i=0;$i -lt 25;$i++) { $tcp=New-Object Net.Sockets.TcpClient;try {$tcp.Connect('127.0.0.1',33319);$ready=$true;break} catch {Start-Sleep -Milliseconds 500} finally {$tcp.Dispose()} }
if (-not $ready) { throw 'Isolated MySQL did not start.' }
$dbPassword=[Guid]::NewGuid().ToString('N');$rootPassword=[Guid]::NewGuid().ToString('N')
$jwt=[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')
[pscustomobject]@{ DB_PASSWORD=($dbPassword|ConvertTo-SecureString -AsPlainText -Force); ROOT_PASSWORD=($rootPassword|ConvertTo-SecureString -AsPlainText -Force); JWT_SECRET=($jwt|ConvertTo-SecureString -AsPlainText -Force) } | Export-Clixml -LiteralPath $secretFile
$sql="CREATE DATABASE safework_alternative_test; CREATE DATABASE safework_legacy_schema; CREATE USER 'safework_alt'@'localhost' IDENTIFIED BY '$dbPassword'; GRANT ALL ON safework_alternative_test.* TO 'safework_alt'@'localhost'; GRANT ALL ON safework_legacy_schema.* TO 'safework_alt'@'localhost'; ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';"
$info=New-Object Diagnostics.ProcessStartInfo
$info.FileName=Join-Path $mysqlBin 'mysql.exe';$info.Arguments='--no-defaults --protocol=TCP --host=127.0.0.1 --port=33319 --user=root --skip-password'
$info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.RedirectStandardInput=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
$client=New-Object Diagnostics.Process;$client.StartInfo=$info
try {$null=$client.Start();$o=$client.StandardOutput.ReadToEndAsync();$e=$client.StandardError.ReadToEndAsync();$client.StandardInput.WriteLine($sql);$client.StandardInput.Close();$client.WaitForExit();if($client.ExitCode -ne 0){throw 'Local provisioning failed; no retry automatically.'};Write-Output 'ISOLATED_MYSQL_READY_127_0_0_1_33319'} finally {$sql=$null;$dbPassword=$null;$rootPassword=$null;$jwt=$null;$client.Dispose()}
