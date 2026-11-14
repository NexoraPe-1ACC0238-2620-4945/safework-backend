[CmdletBinding()]
param([string]$JavaHome='C:\Program Files\Android\Android Studio\jbr',[string]$TestRuntimeRoot,[string]$MavenUserHome,[string]$BuildDirectory)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if(-not $TestRuntimeRoot){$TestRuntimeRoot=Join-Path $repo '.local'}
$TestRuntimeRoot=[IO.Path]::GetFullPath($TestRuntimeRoot)
$c=Import-Clixml -LiteralPath (Join-Path $TestRuntimeRoot 'test-mysql.clixml')
function Plain([Security.SecureString]$v){(New-Object Management.Automation.PSCredential('local',$v)).GetNetworkCredential().Password}
foreach($entry in Get-ChildItem Env:){if($entry.Name -match '^(DB_|SPRING_|BOOTSTRAP_|SAFEWORK_|JWT_|JAVA_TOOL_OPTIONS|JDK_JAVA_OPTIONS|_JAVA_OPTIONS|MAVEN_ARGS)'){Remove-Item -LiteralPath ('Env:'+$entry.Name)}}
$env:JAVA_HOME=$JavaHome
$env:DB_HOST='127.0.0.1';$env:DB_PORT='33319';$env:DB_NAME='safework_alternative_test';$env:DB_USERNAME='safework_alt'
$env:DB_PASSWORD=Plain $c.DB_PASSWORD;$env:JWT_SECRET=Plain $c.JWT_SECRET
$env:APP_PROFILE='dev';$env:DB_TLS='false';$env:DB_ALLOW_PUBLIC_KEY='true';$env:DDL_AUTO='validate';$env:MIGRATIONS_ENABLED='true'
$existingCache=[IO.Path]::GetFullPath((Join-Path $repo '../local-run/maven-home'))
$env:MAVEN_USER_HOME=if($MavenUserHome){[IO.Path]::GetFullPath($MavenUserHome)}elseif(Test-Path -LiteralPath $existingCache){$existingCache}else{Join-Path $repo '.local/maven-home'}
$env:MAVEN_OPTS='-Duser.timezone=UTC'
$env:SAFEWORK_TEST_MYSQL_DATA_DIR=Join-Path $TestRuntimeRoot 'mysql-data'
$log=Join-Path $repo '.local/maven-verify.log'
New-Item -ItemType Directory -Path (Join-Path $repo '.local/runtime') -Force|Out-Null
Set-Location $repo
$verifyArguments=@('clean','verify','-B','-ntp',('-Dmaven.repo.local='+$env:MAVEN_USER_HOME+'\repository'))
if($BuildDirectory){
    $BuildDirectory=[IO.Path]::GetFullPath((Join-Path $repo $BuildDirectory))
    $allowed=[IO.Path]::GetFullPath((Join-Path $repo '.local'))+[IO.Path]::DirectorySeparatorChar
    if(-not $BuildDirectory.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)){throw 'Custom build output must be a child of this repository .local directory.'}
    if((Test-Path -LiteralPath $BuildDirectory) -and ((Get-Item -LiteralPath $BuildDirectory -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)){throw 'Build output must not be a filesystem link.'}
    $verifyArguments+=('-Dsafework.build.directory='+$BuildDirectory)
}
try {
    $ErrorActionPreference='Continue'
    & .\mvnw.cmd @verifyArguments *> $log
    $code=$LASTEXITCODE;$ErrorActionPreference='Stop'
    $text=[IO.File]::ReadAllText($log)
    foreach($v in @($env:DB_PASSWORD,$env:JWT_SECRET)){$text=$text.Replace($v,'[REDACTED]')}
    [IO.File]::WriteAllText($log,$text)
    Write-Output ('MAVEN_VERIFY_EXIT='+$code)
    exit $code
}finally{$env:DB_PASSWORD=$null;$env:JWT_SECRET=$null}
