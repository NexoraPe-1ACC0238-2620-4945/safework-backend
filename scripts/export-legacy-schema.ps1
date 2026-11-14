# Provenance helper used once to obtain V1 from the original verified JAR.
# Not a deployment step. Never regenerate or edit an applied V1 migration.
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$s=Import-Clixml -LiteralPath (Join-Path $repo '.local/test-mysql.clixml')
function Plain([Security.SecureString]$v) { (New-Object Management.Automation.PSCredential('local',$v)).GetNetworkCredential().Password }
$jar=[IO.Path]::GetFullPath((Join-Path $repo '../local-run/backend/target/service-1.0.0-SNAPSHOT.jar'))
$java='C:\Program Files\Android\Android Studio\jbr\bin\java.exe'
$info=New-Object Diagnostics.ProcessStartInfo
$info.FileName=$java;$info.Arguments='-jar "'+$jar+'"';$info.UseShellExecute=$false;$info.CreateNoWindow=$true
$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
foreach($key in @($info.EnvironmentVariables.Keys)) {if($key -match '^(DB_|SPRING_|BOOTSTRAP_|SAFEWORK_|JWT_|JAVA_TOOL_OPTIONS|JDK_JAVA_OPTIONS|_JAVA_OPTIONS)') {$info.EnvironmentVariables.Remove($key)}}
$envs=@{DB_HOST='127.0.0.1';DB_PORT='33319';DB_NAME='safework_legacy_schema';DB_USERNAME='safework_alt';DB_PASSWORD=(Plain $s.DB_PASSWORD);JWT_SECRET=(Plain $s.JWT_SECRET);APP_PROFILE='dev';DB_TLS='false';DB_ALLOW_PUBLIC_KEY='true';DDL_AUTO='update';SERVER_ADDRESS='127.0.0.1';SERVER_PORT='18089';SAFEWORK_BOOTSTRAP_ENABLED='false'}
foreach($key in $envs.Keys) {$info.EnvironmentVariables[$key]=$envs[$key]}
$p=New-Object Diagnostics.Process;$p.StartInfo=$info
try {
    $null=$p.Start();$out=$p.StandardOutput.ReadToEndAsync();$err=$p.StandardError.ReadToEndAsync();$ready=$false
    for($i=0;$i -lt 60;$i++) {if($p.HasExited){break};try {$r=Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:18089/v3/api-docs' -TimeoutSec 1;if($r.StatusCode -eq 200){$ready=$true;break}}catch{};Start-Sleep -Milliseconds 500}
    if(-not $ready){throw 'Legacy schema generator failed; no production database used.'}
} finally {if(-not $p.HasExited){$p.Kill();$p.WaitForExit()};$p.Dispose();$envs.Clear()}
$dump=New-Object Diagnostics.ProcessStartInfo
$dump.FileName='C:\Program Files\MySQL\MySQL Server 9.5\bin\mysqldump.exe'
$dump.Arguments='--no-defaults --host=127.0.0.1 --port=33319 --user=safework_alt --no-data --skip-comments --skip-add-drop-table --no-tablespaces --set-gtid-purged=OFF --skip-lock-tables safework_legacy_schema'
$dump.UseShellExecute=$false;$dump.CreateNoWindow=$true;$dump.RedirectStandardOutput=$true;$dump.RedirectStandardError=$true;$dump.EnvironmentVariables['MYSQL_PWD']=Plain $s.DB_PASSWORD
$p=New-Object Diagnostics.Process;$p.StartInfo=$dump
try {$null=$p.Start();$out=$p.StandardOutput.ReadToEndAsync();$err=$p.StandardError.ReadToEndAsync();$p.WaitForExit();if($p.ExitCode -ne 0){throw 'Schema export failed.'};$ddl=$out.GetAwaiter().GetResult();$ddl=$ddl -replace ' AUTO_INCREMENT=\d+','';$dest=Join-Path $repo 'src/main/resources/db/migration';New-Item -ItemType Directory -Path $dest -Force|Out-Null;[IO.File]::WriteAllText((Join-Path $dest 'V1__legacy_schema.sql'),$ddl);Write-Output ('LEGACY_SCHEMA_TABLES='+[regex]::Matches($ddl,'CREATE TABLE').Count)} finally {$p.Dispose();$dump.EnvironmentVariables.Remove('MYSQL_PWD')}
