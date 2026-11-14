# Private operator-side CA preparation. No database connection is opened here.
function Set-TlsInputFromEnvironment([hashtable]$Child) {
    $keys=@('DB_TRUSTSTORE_URL','DB_TRUSTSTORE_BASE64','DB_CA_CERT_PATH','DB_CA_CERT_BASE64')
    $selected=@($keys | Where-Object {-not [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_))})
    if($selected.Count -ne 1){throw 'Set one certificate source, preferably DB_CA_CERT_PATH pointing to the Aiven ca.pem file.'}
    $key=$selected[0];$Child[$key]=[Environment]::GetEnvironmentVariable($key)
    $password=[Environment]::GetEnvironmentVariable('DB_TRUSTSTORE_PASSWORD')
    if($key -like 'DB_TRUSTSTORE_*' -and [string]::IsNullOrWhiteSpace($password)){throw 'An existing PKCS12 store requires DB_TRUSTSTORE_PASSWORD. Use DB_CA_CERT_PATH for automatic preparation.'}
    if($password){$Child.DB_TRUSTSTORE_PASSWORD=$password}
}
function Initialize-OperatorTruststore([hashtable]$Child,[string]$JavaHome,[string]$Runtime) {
    $files=New-Object Collections.Generic.List[string]
    if($Child.ContainsKey('DB_TRUSTSTORE_URL')){return}
    $store=Join-Path $Runtime ('truststore-'+[Guid]::NewGuid().ToString('N')+'.p12')
    if($Child.ContainsKey('DB_TRUSTSTORE_BASE64')){
        [IO.File]::WriteAllBytes($store,[Convert]::FromBase64String($Child.DB_TRUSTSTORE_BASE64))
        $files.Add($store);$Child.Remove('DB_TRUSTSTORE_BASE64')
    }else{
        if($Child.ContainsKey('DB_CA_CERT_PATH')){
            $pem=[IO.File]::ReadAllText([IO.Path]::GetFullPath($Child.DB_CA_CERT_PATH))
        }else{$pem=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($Child.DB_CA_CERT_BASE64))}
        $certificates=[regex]::Matches($pem,'-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----',[Text.RegularExpressions.RegexOptions]::Singleline)
        if($certificates.Count -eq 0 -or [regex]::Matches($pem,'-----BEGIN CERTIFICATE-----').Count -ne $certificates.Count -or [regex]::Matches($pem,'-----END CERTIFICATE-----').Count -ne $certificates.Count){throw 'The Aiven CA must be a valid PEM certificate or certificate bundle.'}
        if(-not $Child.ContainsKey('DB_TRUSTSTORE_PASSWORD')){$Child.DB_TRUSTSTORE_PASSWORD=[Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N')}
        $files.Add($store)
        $index=0
        foreach($certificate in $certificates){
            $index++;$file=Join-Path $Runtime ('certificate-'+[Guid]::NewGuid().ToString('N')+'.pem');$files.Add($file)
            [IO.File]::WriteAllText($file,$certificate.Value,(New-Object Text.UTF8Encoding($false)))
            $info=New-Object Diagnostics.ProcessStartInfo
            $info.FileName=Join-Path $JavaHome 'bin/keytool.exe'
            $info.Arguments='-importcert -noprompt -alias aiven-ca-'+$index+' -file "'+$file+'" -keystore "'+$store+'" -storetype PKCS12 -storepass:env DB_TRUSTSTORE_PASSWORD'
            $info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
            $info.EnvironmentVariables.Clear()
            foreach($name in @('SystemRoot','WINDIR','PATH','TEMP','TMP','USERPROFILE')){$value=[Environment]::GetEnvironmentVariable($name);if($value){$info.EnvironmentVariables[$name]=$value}}
            $info.EnvironmentVariables['DB_TRUSTSTORE_PASSWORD']=$Child.DB_TRUSTSTORE_PASSWORD
            $p=New-Object Diagnostics.Process;$p.StartInfo=$info
            try{
                $null=$p.Start();$out=$p.StandardOutput.ReadToEndAsync();$err=$p.StandardError.ReadToEndAsync()
                if(-not $p.WaitForExit(30000)){$p.Kill();throw 'Local certificate import timed out.'}
                if($p.ExitCode -ne 0){throw 'The Aiven CA could not be imported; no certificate/password output is printed.'}
            }catch{foreach($temporary in $files){if(Test-Path -LiteralPath $temporary){Remove-Item -LiteralPath $temporary -Force}};throw}finally{$p.Dispose()}
        }
        $pem=$null;$Child.Remove('DB_CA_CERT_PATH');$Child.Remove('DB_CA_CERT_BASE64')
    }
    $Child.DB_TRUSTSTORE_URL=(New-Object Uri([IO.Path]::GetFullPath($store))).AbsoluteUri
    return $files.ToArray()
}
