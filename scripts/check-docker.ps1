$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$docker='C:\Program Files\Docker\Docker\resources\bin\docker.exe'
$runtime=Join-Path $repo '.local';New-Item -ItemType Directory -Path $runtime -Force|Out-Null
$run=[Guid]::NewGuid().ToString('N').Substring(0,10);$network='safework-alt-'+$run
$dbName=$network+'-db';$appName=$network+'-app'
$label='com.nexorape.safework.review='+$run
$dbCreated=$false;$appCreated=$false;$networkCreated=$false
$dbPassword=[Guid]::NewGuid().ToString('N');$rootPassword=[Guid]::NewGuid().ToString('N')
$jwt=[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')
$dbEnv=Join-Path $runtime ('docker-db-'+$run+'.env');$appEnv=Join-Path $runtime ('docker-app-'+$run+'.env')
[IO.File]::WriteAllLines($dbEnv,@('MYSQL_DATABASE=safework_smoke','MYSQL_USER=safework_smoke',('MYSQL_PASSWORD='+$dbPassword),('MYSQL_ROOT_PASSWORD='+$rootPassword)))
[IO.File]::WriteAllLines($appEnv,@('APP_PROFILE=dev',('DB_HOST='+$dbName),'DB_PORT=3306','DB_NAME=safework_smoke','DB_USERNAME=safework_smoke',('DB_PASSWORD='+$dbPassword),('JWT_SECRET='+$jwt),'DB_TLS=false','DB_ALLOW_PUBLIC_KEY=true','DDL_AUTO=validate','MIGRATIONS_ENABLED=false','SERVER_ADDRESS=0.0.0.0','PORT=8080'))
$checks=New-Object Collections.Generic.List[object]
function Http([string]$Label,[string]$Method,[string]$Path,$Body,[string]$Token,[int]$Expected){
    $options=@{Uri='http://127.0.0.1:18087'+$Path;Method=$Method;UseBasicParsing=$true;TimeoutSec=15;Headers=@{}}
    if($Body){$options.Body=$Body|ConvertTo-Json -Compress;$options.ContentType='application/json'}
    if($Token){$options.Headers.Authorization='Bearer '+$Token}
    $response=$null;$status=0
    try{$response=Invoke-WebRequest @options;$status=[int]$response.StatusCode}catch{if($_.Exception.Response){$status=[int]$_.Exception.Response.StatusCode}}
    $checks.Add([pscustomobject]@{label=$Label;method=$Method;path=$Path;status=$status;expected=$Expected;pass=($status -eq $Expected)})
    $checks|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $runtime 'docker-http-results.json') -Encoding UTF8
    if($status -ne $Expected){throw ('Container HTTP failed: '+$Label+' status='+$status)}
    if($response -and $response.Content){return($response.Content|ConvertFrom-Json)}
}
try{
    $null=& $docker network create --label $label $network
    if($LASTEXITCODE -ne 0){throw 'Local Docker network creation failed.'};$networkCreated=$true
    $null=& $docker run -d --name $dbName --label $label --network $network --memory 768m --env-file $dbEnv mysql:8.4 --innodb-buffer-pool-size=64M --max-connections=30 2> (Join-Path $runtime 'docker-mysql-start.log')
    if($LASTEXITCODE -ne 0){throw 'Isolated MySQL container start failed.'};$dbCreated=$true
    $ready=$false
    for($i=0;$i -lt 90;$i++){
        $ErrorActionPreference='Continue';$log=& $docker logs $dbName 2>&1;$ErrorActionPreference='Stop'
        if(($log -join "`n") -match 'ready for connections.*port: 3306'){$ready=$true;break}
        Start-Sleep -Milliseconds 500
    }
    if(-not $ready){throw 'Isolated MySQL container not ready.'}
    Write-Output 'DOCKER_MYSQL_8_4_READY'
    $ErrorActionPreference='Continue'
    & $docker run --rm --label $label --network $network --memory 768m --env-file $appEnv -e SAFEWORK_MIGRATE_ENABLED=true safework-backend-alternative:review *> (Join-Path $runtime 'docker-migration.log')
    $migrationExit=$LASTEXITCODE;$ErrorActionPreference='Stop'
    if($migrationExit -ne 0){throw 'Container migration failed.'}
    $null=& $docker run -d --name $appName --label $label --network $network --memory 768m --env-file $appEnv -p '127.0.0.1:18087:8080' safework-backend-alternative:review
    if($LASTEXITCODE -ne 0){throw 'Application container start failed.'};$appCreated=$true
    $ready=$false
    for($i=0;$i -lt 60;$i++){try{$r=Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:18087/health' -TimeoutSec 2;if($r.StatusCode -eq 200){$ready=$true;break}}catch{};Start-Sleep -Milliseconds 500}
    if(-not $ready){throw 'Application container not ready.'}
    $null=Http 'container health' GET '/health' $null '' 200
    $null=Http 'container OpenAPI' GET '/v3/api-docs' $null '' 200
    $password='Container-'+[Guid]::NewGuid().ToString('N').Substring(0,8)+'!'
    $email='container.employer@example.test';$worker='container.worker@example.test'
    $null=Http 'container signup' POST '/api/v1/authentication/sign-up' @{fullName='Synthetic Container Employer';emailAddress=$email;password=$password} '' 201
    $login=Http 'container login' POST '/api/v1/authentication/sign-in' @{email=$email;password=$password} '' 200;$token=$login.token
    $company=Http 'container company without ADMIN' POST '/api/v1/companies' @{name='Synthetic Container Company'} $token 201
    $null=Http 'container creator session revoked' GET '/api/v1/users/me' $null $token 401
    $login=Http 'container employer renewed login' POST '/api/v1/authentication/sign-in' @{email=$email;password=$password} '' 200;$token=$login.token
    $null=Http 'container worker signup' POST '/api/v1/authentication/sign-up' @{fullName='Synthetic Container Worker';emailAddress=$worker;password=$password} '' 201
    $login=Http 'container unassigned login' POST '/api/v1/authentication/sign-in' @{email=$worker;password=$password} '' 200;$workerToken=$login.token
    $null=Http 'container pending access denied' GET '/api/v1/incidents' $null $workerToken 403
    $request=Http 'container request without code' POST ('/api/v1/companies/'+$company.id+'/membership-requests') $null $workerToken 201
    $null=Http 'container employer approval' POST ('/api/v1/companies/'+$company.id+'/membership-requests/'+$request.id+'/accept') $null $token 200
    $null=Http 'container admission session revoked' GET '/api/v1/users/me' $null $workerToken 401
    $login=Http 'container approved login' POST '/api/v1/authentication/sign-in' @{email=$worker;password=$password} '' 200;$workerToken=$login.token
    $null=Http 'container approved incidents' GET '/api/v1/incidents' $null $workerToken 200
    $null=Http 'container logout' POST '/api/v1/authentication/sign-out' $null $workerToken 204
    [ordered]@{image='safework-backend-alternative:review';imageId=(& $docker image inspect safework-backend-alternative:review --format '{{.Id}}');mysqlImageId=(& $docker image inspect mysql:8.4 --format '{{.Id}}');migrationExit=$migrationExit;httpChecks=$checks.Count;passed=$true}|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $runtime 'docker-verification.json') -Encoding UTF8
    Write-Output ('DOCKER_HTTP_CHECKS_PASSED='+$checks.Count)
}finally{
    # Only this run's named, labelled resources may be removed. Never prune Docker.
    foreach($entry in @(@($appName,$appCreated),@($dbName,$dbCreated))){if($entry[1]){
        $labels=(& $docker inspect $entry[0] --format '{{json .Config.Labels}}')|ConvertFrom-Json
        if($labels.'com.nexorape.safework.review' -eq $run){$null=& $docker rm -f -v $entry[0]}
    }}
    if($networkCreated){$null=& $docker network rm $network}
    # Remove only the two generated credential files, whose absolute paths are in .local.
    foreach($file in @($dbEnv,$appEnv)){if([IO.Path]::GetFullPath($file).StartsWith([IO.Path]::GetFullPath($runtime)+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){Remove-Item -LiteralPath $file -Force}}
    $dbPassword=$null;$rootPassword=$null;$jwt=$null;$password=$null;$token=$null;$workerToken=$null
}
