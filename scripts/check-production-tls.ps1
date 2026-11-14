$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$docker='C:\Program Files\Docker\Docker\resources\bin\docker.exe'
$openssl='C:\Program Files\Git\usr\bin\openssl.exe'
$runtime=Join-Path $repo '.local'
$run=[Guid]::NewGuid().ToString('N').Substring(0,10)
$network='safework-tls-'+$run;$db=$network+'-db';$app=$network+'-app';$label='com.nexorape.safework.tls-review='+$run
$folder=Join-Path $runtime ('tls-'+$run);New-Item -ItemType Directory -Path $folder|Out-Null
$checks=New-Object Collections.Generic.List[object]
$dbPassword=[Guid]::NewGuid().ToString('N');$rootPassword=[Guid]::NewGuid().ToString('N')
$jwt=[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')
$dbEnv=Join-Path $folder 'db.env';$appEnv=Join-Path $folder 'app.env';$volume=$network+'-certs'
$networkCreated=$false;$volumeCreated=$false
function OpenSsl([string[]]$Parameters){
    $ErrorActionPreference='Continue'
    & $openssl @Parameters *> (Join-Path $folder 'openssl.log')
    if($LASTEXITCODE -ne 0){throw 'Synthetic TLS certificate generation failed.'}
}
try{
    OpenSsl @('req','-x509','-newkey','rsa:2048','-nodes','-keyout',(Join-Path $folder 'ca-key.pem'),'-out',(Join-Path $folder 'ca.pem'),'-days','1','-subj','/CN=SafeWork-Isolated-Test-CA','-addext','basicConstraints=critical,CA:TRUE')
    OpenSsl @('req','-newkey','rsa:2048','-nodes','-keyout',(Join-Path $folder 'server-key.pem'),'-out',(Join-Path $folder 'server.csr'),'-subj',('/CN='+$db))
    [IO.File]::WriteAllText((Join-Path $folder 'server.ext'),"subjectAltName=DNS:$db`nextendedKeyUsage=serverAuth`nbasicConstraints=CA:FALSE`n")
    OpenSsl @('x509','-req','-in',(Join-Path $folder 'server.csr'),'-CA',(Join-Path $folder 'ca.pem'),'-CAkey',(Join-Path $folder 'ca-key.pem'),'-CAcreateserial','-out',(Join-Path $folder 'server-cert.pem'),'-days','1','-extfile',(Join-Path $folder 'server.ext'))
    [IO.File]::WriteAllLines($dbEnv,@('MYSQL_DATABASE=safework_tls','MYSQL_USER=safework_tls',('MYSQL_PASSWORD='+$dbPassword),('MYSQL_ROOT_PASSWORD='+$rootPassword)))
    [IO.File]::WriteAllLines($appEnv,@('APP_PROFILE=prod',('DB_HOST='+$db),'DB_PORT=3306','DB_NAME=safework_tls','DB_USERNAME=safework_tls',('DB_PASSWORD='+$dbPassword),('JWT_SECRET='+$jwt),'DDL_AUTO=validate','MIGRATIONS_ENABLED=false','PORT=8080'))
    $null=& $docker network create --label $label $network;if($LASTEXITCODE -ne 0){throw 'TLS network creation failed'};$networkCreated=$true
    $null=& $docker volume create --label $label $volume;if($LASTEXITCODE -ne 0){throw 'TLS volume creation failed'};$volumeCreated=$true
    $sourceMount='type=bind,source='+$folder+',target=/source,readonly'
    $copy='set -eu; cp /source/ca.pem /source/server-cert.pem /source/server-key.pem /tls/; chown mysql:mysql /tls/*; chmod 600 /tls/server-key.pem; chmod 644 /tls/ca.pem /tls/server-cert.pem'
    $ErrorActionPreference='Continue'
    & $docker run --rm --network none --mount $sourceMount -v ($volume+':/tls') --entrypoint /bin/sh mysql:8.4 -c $copy *> (Join-Path $folder 'certificate-copy.log')
    $copyExit=$LASTEXITCODE;$ErrorActionPreference='Stop';if($copyExit -ne 0){throw 'TLS fixture volume setup failed'}
    $null=& $docker run -d --name $db --label $label --network $network --network-alias ($db+'-wrong') --memory 768m --env-file $dbEnv -v ($volume+':/tls:ro') mysql:8.4 --innodb-buffer-pool-size=64M --max-connections=30 --ssl-ca=/tls/ca.pem --ssl-cert=/tls/server-cert.pem --ssl-key=/tls/server-key.pem --require-secure-transport=ON
    if($LASTEXITCODE -ne 0){throw 'TLS MySQL start failed'}
    $ready=$false
    for($i=0;$i -lt 100;$i++){
        $ErrorActionPreference='Continue';$log=& $docker logs $db 2>&1;$ErrorActionPreference='Stop'
        if(($log -join "`n") -match 'ready for connections.*port: 3306'){$ready=$true;break}
        Start-Sleep -Milliseconds 500
    }
    if(-not $ready){throw 'TLS MySQL not ready'}
    $caMount='type=bind,source='+(Join-Path $folder 'ca.pem')+',target=/etc/secrets/ca.pem,readonly'
    $ErrorActionPreference='Continue'
    & $docker run --rm --network $network --memory 768m --env-file $appEnv --mount $caMount -e SAFEWORK_MIGRATE_ENABLED=true safework-backend-alternative:review *> (Join-Path $runtime 'production-tls-migration.log')
    $migration=$LASTEXITCODE;$ErrorActionPreference='Stop'
    if($migration -ne 0){throw 'Production-profile migration with automatic ca.pem import failed'}
    $checks.Add([pscustomobject]@{check='productionMigrationWithVerifiedIdentity';pass=$true})
    Write-Output 'PRODUCTION_TLS_MIGRATION_WITH_CA_PEM_PASSED'
    $null=& $docker run -d --name $app --label $label --network $network --memory 768m --env-file $appEnv --mount $caMount -p '127.0.0.1:18090:8080' safework-backend-alternative:review
    if($LASTEXITCODE -ne 0){throw 'TLS application start failed'}
    $ready=$false
    for($i=0;$i -lt 75;$i++){try{$r=Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:18090/health' -TimeoutSec 2;if($r.StatusCode -eq 200){$ready=$true;break}}catch{};Start-Sleep -Milliseconds 500}
    if(-not $ready){throw 'TLS application not ready'}
    $checks.Add([pscustomobject]@{check='productionStartupWithOnlyRenderCaPemFile';pass=$true})
    $password='Tls-'+[Guid]::NewGuid().ToString('N')
    $body=@{fullName='Synthetic TLS Account';emailAddress='tls-account@example.test';password=$password}|ConvertTo-Json -Compress
    $r=Invoke-WebRequest -UseBasicParsing -Method Post -Uri 'http://127.0.0.1:18090/api/v1/authentication/sign-up' -ContentType 'application/json' -Body $body
    if($r.StatusCode -ne 201){throw 'TLS signup failed'}
    $checks.Add([pscustomobject]@{check='productionSignupOverVerifiedDatabaseTls';pass=$true})
    $login=Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:18090/api/v1/authentication/sign-in' -ContentType 'application/json' -Body (@{email='tls-account@example.test';password=$password}|ConvertTo-Json -Compress)
    $r=Invoke-WebRequest -UseBasicParsing -Method Post -Uri 'http://127.0.0.1:18090/api/v1/authentication/sign-out' -Headers @{Authorization='Bearer '+$login.token}
    if($r.StatusCode -ne 204){throw 'TLS logout failed'}
    $checks.Add([pscustomobject]@{check='productionLoginAndLogout';pass=$true})
    $ErrorActionPreference='Continue'
    & $docker run --rm --network $network --memory 768m --env-file $appEnv --mount $caMount -e ('DB_HOST='+$db+'-wrong') -e SAFEWORK_MIGRATE_ENABLED=true safework-backend-alternative:review *> (Join-Path $runtime 'production-tls-wrong-host.log')
    $wrongHost=$LASTEXITCODE;$ErrorActionPreference='Stop'
    if($wrongHost -eq 0){throw 'The mismatched certificate hostname was accepted'}
    $hostLog=[IO.File]::ReadAllText((Join-Path $runtime 'production-tls-wrong-host.log'))
    if($hostLog -notmatch '(?i)subject alternative|identity|identity verification|certificate|SSLHandshakeException'){throw 'Wrong-host fixture failed for an unexpected reason'}
    $checks.Add([pscustomobject]@{check='mismatchedCertificateHostnameRejected';pass=$true})
    $checks|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $runtime 'production-tls-verification.json') -Encoding UTF8
    Write-Output ('PRODUCTION_TLS_CHECKS_PASSED='+$checks.Count)
}finally{
    $ErrorActionPreference='Continue'
    foreach($name in @($app,$db)){
        $json=& $docker inspect $name --format '{{json .Config.Labels}}' 2>$null
        if($LASTEXITCODE -eq 0){$labels=$json|ConvertFrom-Json;if($labels.'com.nexorape.safework.tls-review' -eq $run){$null=& $docker rm -f -v $name}}
    }
    if($networkCreated){$null=& $docker network rm $network}
    if($volumeCreated){$null=& $docker volume rm $volume}
    # The fixture directory was created by this run and contains only synthetic data.
    $allowed=[IO.Path]::GetFullPath($runtime)+[IO.Path]::DirectorySeparatorChar
    if([IO.Path]::GetFullPath($folder).StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)){
        Get-ChildItem -LiteralPath $folder -File|ForEach-Object{Remove-Item -LiteralPath $_.FullName -Force}
        [IO.Directory]::Delete($folder,$false)
    }
    $dbPassword=$null;$rootPassword=$null;$jwt=$null;$password=$null;$login=$null
}
