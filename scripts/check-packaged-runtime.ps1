[CmdletBinding()]
param([switch]$Resume)
$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$runtime=Join-Path $repo '.local';$base='http://127.0.0.1:18086/api/v1'
$secretFile=Join-Path $runtime 'demo-accounts.private.clixml'
if((Test-Path -LiteralPath $secretFile) -and -not $Resume){throw 'Packaged-runtime fixtures already exist; inspect them before explicitly using -Resume.'}
if($Resume -and -not(Test-Path -LiteralPath $secretFile)){throw 'No previous synthetic fixture to resume.'}
$reports=New-Object Collections.Generic.List[object]
if($Resume){foreach($entry in (Get-Content -LiteralPath (Join-Path $runtime 'packaged-http-results.json') -Raw|ConvertFrom-Json)){$reports.Add($entry)}}
function Call([string]$Label,[string]$Method,[string]$Path,$Body,[string]$Token,[int]$Expected){
    $options=@{Uri=$base+$Path;Method=$Method;UseBasicParsing=$true;TimeoutSec=20;Headers=@{}}
    if($Token){$options.Headers.Authorization='Bearer '+$Token}
    if($null -ne $Body){$options.Body=$Body|ConvertTo-Json -Depth 6 -Compress;$options.ContentType='application/json'}
    $response=$null;$status=0
    try{$response=Invoke-WebRequest @options;$status=[int]$response.StatusCode}catch{if($_.Exception.Response){$status=[int]$_.Exception.Response.StatusCode}}
    $reports.Add([pscustomobject]@{label=$Label;method=$Method;path=$Path;status=$status;expected=$Expected;pass=($status -eq $Expected)})
    $reports|ConvertTo-Json -Depth 4|Set-Content -LiteralPath (Join-Path $runtime 'packaged-http-results.json') -Encoding UTF8
    if($status -ne $Expected){throw ('Packaged HTTP failure: '+$Label+' status='+$status+' expected='+$Expected)}
    if($response -and $response.Content){return ($response.Content|ConvertFrom-Json)}
}
$password='SafeWork-'+[Guid]::NewGuid().ToString('N').Substring(0,8)+'!'
$employer='employer.alternative@example.test';$worker='worker.alternative@example.test'
if($Resume){$saved=Import-Clixml -LiteralPath $secretFile;$password=(New-Object Management.Automation.PSCredential('local',$saved.password)).GetNetworkCredential().Password}
else{[pscustomobject]@{employerEmail=$employer;workerEmail=$worker;password=($password|ConvertTo-SecureString -AsPlainText -Force);base=$base}|Export-Clixml -LiteralPath $secretFile}
try{
    if(-not $Resume){
    foreach($email in @($employer,$worker)){$null=Call 'signup without code' POST '/authentication/sign-up' @{fullName='Synthetic Alternative';emailAddress=$email;password=$password} '' 201}
    $login=Call 'employer initial login' POST '/authentication/sign-in' @{email=$employer;password=$password} '' 200;$employerToken=$login.token
    $company=Call 'company creation without ADMIN' POST '/companies' @{name='SafeWork Alternative Demo'} $employerToken 201
    $null=Call 'creation revokes old session' GET '/users/me' $null $employerToken 401
    $login=Call 'employer renewed login' POST '/authentication/sign-in' @{email=$employer;password=$password} '' 200;$employerToken=$login.token
    $login=Call 'unassigned worker login' POST '/authentication/sign-in' @{email=$worker;password=$password} '' 200;$workerToken=$login.token
    $null=Call 'pre-approval incident denial' GET '/incidents' $null $workerToken 403
    $null=Call 'minimal company search' GET '/companies?search=Alternative' $null $workerToken 200
    $request=Call 'request without code' POST ('/companies/'+$company.id+'/membership-requests') $null $workerToken 201
    $null=Call 'approve worker' POST ('/companies/'+$company.id+'/membership-requests/'+$request.id+'/accept') $null $employerToken 200
    $null=Call 'approval revokes old session' GET '/users/me' $null $workerToken 401
    $login=Call 'approved worker login' POST '/authentication/sign-in' @{email=$worker;password=$password} '' 200;$workerToken=$login.token
    $null=Call 'approved worker profile' GET '/users/me' $null $workerToken 200
    $null=Call 'create demo incident' POST '/incidents' @{title='Alternative backend demo';description='Synthetic open incident';location='Local demonstration'} $workerToken 201
    $null=Call 'logout employer fixture' POST '/authentication/sign-out' $null $employerToken 204
    $null=Call 'logout worker fixture' POST '/authentication/sign-out' $null $workerToken 204
    }
    $probeEmail='recovery-probe-'+[Guid]::NewGuid().ToString('N').Substring(0,8)+'@example.test'
    $probe=Call 'recovery fixture signup' POST '/authentication/sign-up' @{fullName='Synthetic Recovery Probe';emailAddress=$probeEmail;password=$password} '' 201
    $login=Call 'pre-recovery login' POST '/authentication/sign-in' @{email=$probeEmail;password=$password} '' 200;$oldToken=$login.token
    $config=Import-Clixml -LiteralPath (Join-Path $runtime 'test-mysql.clixml')
    $env:DB_HOST='127.0.0.1';$env:DB_PORT='33319';$env:DB_NAME='safework_alternative_test';$env:DB_USERNAME='safework_alt'
    $env:DB_PASSWORD=(New-Object Management.Automation.PSCredential('local',$config.DB_PASSWORD)).GetNetworkCredential().Password
    $env:JWT_SECRET=(New-Object Management.Automation.PSCredential('local',$config.JWT_SECRET)).GetNetworkCredential().Password
    $uuidLine=Get-Content -LiteralPath (Join-Path $runtime 'mysql-data/auto.cnf')|Where-Object{$_ -match '^server-uuid='}
    $uuid=$uuidLine.Substring('server-uuid='.Length).Trim()
    $replacement='Recovered-'+[Guid]::NewGuid().ToString('N').Substring(0,8)+'!'
    # Replace interactive input only for this synthetic test. The actual operator
    # script, packaged Java main, database transaction and revocation run unchanged.
    & {
        param($operatorFile,$email,$id,$uuid,$replacement)
        function Read-Host([string]$Prompt,[switch]$AsSecureString){if($AsSecureString){return(ConvertTo-SecureString $replacement -AsPlainText -Force)};return '127.0.0.1:33319/safework_alternative_test'}
        . $operatorFile -Action ResetPassword -JavaHome 'C:\Program Files\Android\Android Studio\jbr' -Profile dev -Email $email -UserId $id -ServerUuid $uuid -Execute
    } (Join-Path $PSScriptRoot 'operator.ps1') $probeEmail $probe.id $uuid $replacement
    $null=Call 'recovery revokes previous session' GET '/users/me' $null $oldToken 401
    $null=Call 'recovery rejects old password' POST '/authentication/sign-in' @{email=$probeEmail;password=$password} '' 401
    $login=Call 'recovery accepts new password' POST '/authentication/sign-in' @{email=$probeEmail;password=$replacement} '' 200;$newToken=$login.token
    $denied=$false
    try{& {
        param($operatorFile,$email,$id,$replacement)
        function Read-Host([string]$Prompt,[switch]$AsSecureString){if($AsSecureString){return(ConvertTo-SecureString $replacement -AsPlainText -Force)};return '127.0.0.1:33319/safework_alternative_test'}
        . $operatorFile -Action ResetPassword -JavaHome 'C:\Program Files\Android\Android Studio\jbr' -Profile dev -Email $email -UserId $id -ServerUuid '00000000-0000-0000-0000-000000000000' -Execute
    } (Join-Path $PSScriptRoot 'operator.ps1') $probeEmail $probe.id ($replacement+'x')}catch{$denied=$true}
    if(-not $denied){throw 'Wrong database identity was accepted.'}
    $null=Call 'wrong UUID reset preserved session' GET '/users/me' $null $newToken 200
    $null=Call 'logout recovery fixture' POST '/authentication/sign-out' $null $newToken 204
    $file=Join-Path $runtime 'DEMO-ACCESS.private.txt'
    [IO.File]::WriteAllText($file,"SafeWork Alternative - LOCAL ONLY`r`nBackend: http://127.0.0.1:18086/`r`nCompany: SafeWork Alternative Demo`r`nEMPLOYER: $employer`r`nWORKER: $worker`r`nPassword for these synthetic accounts: $password`r`n")
    Write-Output ('PACKAGED_HTTP_CHECKS_PASSED='+$reports.Count)
    Write-Output 'CLI_PASSWORD_RECOVERY_AND_WRONG_UUID_GUARD_VERIFIED'
}finally{$password=$null;$replacement=$null;$employerToken=$null;$workerToken=$null;$oldToken=$null;$newToken=$null;$env:DB_PASSWORD=$null;$env:JWT_SECRET=$null}
