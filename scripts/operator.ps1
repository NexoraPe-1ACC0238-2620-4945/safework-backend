[CmdletBinding()]
param(
    [ValidateSet('Migrate','ResetPassword')][string]$Action,
    [string]$JavaHome=$env:JAVA_HOME,
    [string]$JarPath,
    [ValidateSet('dev','prod')][string]$Profile='prod',
    [switch]$BaselineLegacy,
    [string]$Email,
    [long]$UserId,
    [string]$ServerUuid,
    [switch]$Execute
)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if(-not $Action){throw 'Specify -Action Migrate or -Action ResetPassword.'}
if(-not $JarPath){$JarPath=Join-Path $repo 'target/service-1.0.0-SNAPSHOT.jar'}
if(-not $JavaHome){$JavaHome='C:\Program Files\Android\Android Studio\jbr'}
$java=Join-Path $JavaHome 'bin/java.exe'
if(-not(Test-Path -LiteralPath $java) -or -not(Test-Path -LiteralPath $JarPath)){throw 'Set JavaHome and JarPath to the reviewed JDK 25 and JAR.'}
if((Get-Content -LiteralPath (Join-Path $JavaHome 'release') -Raw) -notmatch 'JAVA_VERSION="25[.\-"]'){throw 'Use JDK 25.'}
$child=@{}
foreach($name in @('DB_HOST','DB_PORT','DB_NAME','DB_USERNAME','DB_PASSWORD','JWT_SECRET')){
    $value=[Environment]::GetEnvironmentVariable($name)
    if(-not $value){throw ('Set '+$name+' privately in this terminal first. Spring does not load .env automatically.')}
    $child[$name]=$value
}
if($child.DB_HOST -notmatch '^[a-zA-Z0-9.-]+$' -or $child.DB_NAME -notmatch '^[a-zA-Z0-9_]+$' -or $child.DB_PORT -notmatch '^\d{1,5}$'){throw 'Invalid database destination.'}
if($Profile -eq 'dev' -and $child.DB_HOST -notin @('127.0.0.1','localhost')){throw 'The dev profile is restricted to loopback in this operator tool.'}
if($BaselineLegacy -and $Action -ne 'Migrate'){throw 'BaselineLegacy is only valid for a reviewed legacy migration.'}
$child.APP_PROFILE=$Profile;$child.DDL_AUTO='validate';$child.MIGRATIONS_ENABLED='false';$child.TZ='UTC'
if($Profile -eq 'prod'){
    . (Join-Path $PSScriptRoot 'prepare-tls.ps1')
    Set-TlsInputFromEnvironment $child
}else{$child.DB_TLS='false';$child.DB_ALLOW_PUBLIC_KEY='true'}
$destination='{0}:{1}/{2}' -f $child.DB_HOST,$child.DB_PORT,$child.DB_NAME
Write-Host ('Proposed '+$Action+' destination: '+$destination)
if(-not $Execute){Write-Host 'Preparation only. No process or database operation was started.';return}
if((Read-Host ('Type the exact destination to confirm '+$destination)) -cne $destination){throw 'Destination not confirmed.'}
$secure=$null;$repeat=$null;$newPassword=$null;$p=$null;$temporaryTls=@()
$stamp=[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss-ffff')
$runtime=Join-Path $repo '.local';New-Item -ItemType Directory -Path $runtime -Force|Out-Null
try{
    if($Profile -eq 'prod'){$temporaryTls=@(Initialize-OperatorTruststore $child $JavaHome $runtime)}
    if($Action -eq 'Migrate'){
        $child.SAFEWORK_MIGRATE_ENABLED='true'
        if($BaselineLegacy){
            if((Read-Host 'After backup and schema preflight, type BASELINE_REVIEWED_V1') -cne 'BASELINE_REVIEWED_V1'){throw 'Legacy baseline not confirmed.'}
            $child.SAFEWORK_BASELINE_LEGACY_SCHEMA='true'
        }
    }else{
        if(-not $Email -or $UserId -le 0 -or $ServerUuid -notmatch '^[a-fA-F0-9-]{36}$'){throw 'Provide the exact Email, UserId and ServerUuid from the read-only preflight.'}
        $secure=Read-Host 'New password (hidden)' -AsSecureString;$repeat=Read-Host 'Repeat password (hidden)' -AsSecureString
        $newPassword=(New-Object Management.Automation.PSCredential('local',$secure)).GetNetworkCredential().Password
        $confirm=(New-Object Management.Automation.PSCredential('local',$repeat)).GetNetworkCredential().Password
        if($newPassword -cne $confirm){throw 'Passwords do not match.'}
        if($newPassword -notmatch '^[\x21-\x7e]{12,64}$'){throw 'Use 12-64 visible ASCII characters to satisfy the backend byte limit.'}
        $confirm=$null
        $child.SAFEWORK_PASSWORD_RESET_ENABLED='true';$child.RESET_EMAIL=$Email;$child.RESET_PASSWORD=$newPassword
        $child.RESET_EXPECTED_DATABASE=$child.DB_NAME;$child.RESET_EXPECTED_SERVER_UUID=$ServerUuid;$child.RESET_EXPECTED_USER_ID=[string]$UserId
        $child.RESET_OPERATOR=[Security.Principal.WindowsIdentity]::GetCurrent().Name
        [pscustomobject]@{email=$Email;userId=$UserId;destination=$destination;password=$secure;state='candidate-not-proof-of-success'}|Export-Clixml -LiteralPath (Join-Path $runtime ('recovery-'+$stamp+'.private.clixml'))
    }
    $info=New-Object Diagnostics.ProcessStartInfo
    $info.FileName=$java;$info.Arguments='-Duser.timezone=UTC -jar "'+[IO.Path]::GetFullPath($JarPath)+'"';$info.WorkingDirectory=$repo
    $info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
    $info.EnvironmentVariables.Clear()
    foreach($key in @('SystemRoot','WINDIR','PATH','TEMP','TMP','USERPROFILE','APPDATA','LOCALAPPDATA')){$value=[Environment]::GetEnvironmentVariable($key);if($value){$info.EnvironmentVariables[$key]=$value}}
    foreach($key in $child.Keys){$info.EnvironmentVariables[$key]=[string]$child[$key]}
    $p=New-Object Diagnostics.Process;$p.StartInfo=$info
    $null=$p.Start();$out=$p.StandardOutput.ReadToEndAsync();$err=$p.StandardError.ReadToEndAsync()
    $timedOut=-not $p.WaitForExit(180000)
    if($timedOut){$p.Kill();$p.WaitForExit()}
    $text=$out.GetAwaiter().GetResult()+[Environment]::NewLine+$err.GetAwaiter().GetResult()
    foreach($v in @($child.DB_PASSWORD,$child.JWT_SECRET,$child['DB_TRUSTSTORE_PASSWORD'],$newPassword)){if($v){$text=$text.Replace($v,'[REDACTED]')}}
    [IO.File]::WriteAllText((Join-Path $runtime ('operator-'+$stamp+'.log')),$text)
    [pscustomobject]@{action=$Action;destination=$destination;userId=$UserId;exitCode=$p.ExitCode;timedOut=$timedOut;occurredAtUtc=[DateTime]::UtcNow.ToString('o')}|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $runtime ('operator-'+$stamp+'.json'))
    if($timedOut -or $p.ExitCode -ne 0){throw 'Operation failed or uncertain. Review the private log and database state; do not retry automatically.'}
    Write-Host 'Operation completed with exit code 0. Perform the documented HTTP checks.'
}finally{$child.Clear();$newPassword=$null;if($secure){$secure.Dispose()};if($repeat){$repeat.Dispose()};if($p){$p.Dispose()};foreach($temporary in $temporaryTls){if((Test-Path -LiteralPath $temporary) -and [IO.Path]::GetFullPath($temporary).StartsWith([IO.Path]::GetFullPath($runtime)+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){Remove-Item -LiteralPath $temporary -Force}}}
