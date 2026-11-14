$ErrorActionPreference='Stop'
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$docker='C:\Program Files\Docker\Docker\resources\bin\docker.exe'
$file=Join-Path $repo '.local/entrypoint-test.env';$log=Join-Path $repo '.local/entrypoint-test.log'
try{
    $ErrorActionPreference='Continue'
    & $docker run --rm --network none safework-backend-alternative:review -version *> $log
    $plainExit=$LASTEXITCODE;$ErrorActionPreference='Stop'
    if($plainExit -ne 0){throw 'Plain Java entrypoint failed.'}
    $encoded=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('synthetic-certificate-container-test'))
    [IO.File]::WriteAllLines($file,@(('DB_TRUSTSTORE_BASE64='+$encoded)))
    $script='set -eu; /app/docker-entrypoint.sh -version >/dev/null 2>&1; test $(find /tmp -maxdepth 2 -name ca.p12 | wc -l) -eq 1; test $(stat -c %a /tmp/safework-trust.*/ca.p12) -eq 600; grep -q synthetic-certificate-container-test /tmp/safework-trust.*/ca.p12'
    $ErrorActionPreference='Continue'
    & $docker run --rm --network none --env-file $file --entrypoint /bin/sh safework-backend-alternative:review -c $script *> $log
    $decodeExit=$LASTEXITCODE;$ErrorActionPreference='Stop'
    if($decodeExit -ne 0){throw 'Truststore decoding/content/owner-only permission test failed.'}
    [IO.File]::WriteAllLines($file,@('DB_TRUSTSTORE_BASE64=invalid!base64'))
    $ErrorActionPreference='Continue'
    & $docker run --rm --network none --env-file $file safework-backend-alternative:review -version *> $log
    $invalidExit=$LASTEXITCODE;$ErrorActionPreference='Stop'
    if($invalidExit -eq 0){throw 'Invalid truststore encoding was accepted.'}
    [IO.File]::WriteAllLines($file,@(('DB_TRUSTSTORE_BASE64='+$encoded),'DB_TRUSTSTORE_URL=file:///not-used.p12'))
    $ErrorActionPreference='Continue'
    & $docker run --rm --network none --env-file $file safework-backend-alternative:review -version *> $log
    $conflictExit=$LASTEXITCODE;$ErrorActionPreference='Stop'
    if($conflictExit -ne 64){throw 'Conflicting truststore settings were accepted.'}
    [ordered]@{plainJava=$true;decodeContentAndOwnerOnlyPermissions=$true;invalidBase64Rejected=$true;conflictingConfigurationRejected=$true}|ConvertTo-Json|Set-Content -LiteralPath (Join-Path $repo '.local/entrypoint-verification.json')
    Write-Output 'DOCKER_ENTRYPOINT_4_CHECKS_PASSED'
}finally{if(Test-Path -LiteralPath $file){Remove-Item -LiteralPath $file -Force}}
