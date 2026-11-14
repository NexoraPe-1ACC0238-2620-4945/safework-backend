#!/bin/sh
set -eu

fail() { echo "$1" >&2; exit 64; }
trust_url="${DB_TRUSTSTORE_URL:-}"
trust_base64="${DB_TRUSTSTORE_BASE64:-}"
ca_path="${DB_CA_CERT_PATH:-}"
ca_base64="${DB_CA_CERT_BASE64:-}"
inputs=0
for value in "$trust_url" "$trust_base64" "$ca_path" "$ca_base64"; do
    if [ -n "$value" ]; then inputs=$((inputs + 1)); fi
done
[ "$inputs" -le 1 ] || fail "Configure only one certificate source: ca.pem, DB_CA_CERT_PATH, DB_CA_CERT_BASE64, DB_TRUSTSTORE_URL or DB_TRUSTSTORE_BASE64."
if [ "$inputs" -eq 0 ] && [ -r /etc/secrets/ca.pem ]; then
    ca_path=/etc/secrets/ca.pem
fi

if [ -n "$trust_base64" ] || [ -n "$ca_path" ] || [ -n "$ca_base64" ]; then
    umask 077
    trust_dir="$(mktemp -d "${TMPDIR:-/tmp}/safework-trust.XXXXXX")"
    if [ -n "$trust_base64" ]; then
        printf '%s' "$trust_base64" | base64 -d > "$trust_dir/ca.p12" ||
            fail "DB_TRUSTSTORE_BASE64 is not valid base64."
    else
        if [ -n "$ca_base64" ]; then
            printf '%s' "$ca_base64" | base64 -d > "$trust_dir/source.pem" ||
                fail "DB_CA_CERT_BASE64 is not valid base64."
            ca_path="$trust_dir/source.pem"
        fi
        [ -r "$ca_path" ] || fail "The configured Aiven CA certificate file is not readable."
        command -v keytool >/dev/null 2>&1 || fail "The Java runtime must include keytool to import the Aiven CA."
        if [ -z "${DB_TRUSTSTORE_PASSWORD:-}" ]; then
            DB_TRUSTSTORE_PASSWORD="$(head -c 32 /dev/urandom | base64 | tr -d '\n')"
            export DB_TRUSTSTORE_PASSWORD
        fi
        # Import every certificate in the Aiven rotation bundle.
        awk -v dir="$trust_dir" '
            /-----BEGIN CERTIFICATE-----/ { n++; active=1; file=dir "/certificate-" n ".pem" }
            active { print > file }
            /-----END CERTIFICATE-----/ { close(file); active=0 }
            END { if(n==0 || active) exit 1 }
        ' "$ca_path" || fail "Provide the PEM CA certificate downloaded from the same Aiven service."
        index=0
        for certificate in "$trust_dir"/certificate-*.pem; do
            index=$((index + 1))
            keytool -importcert -noprompt -alias "aiven-ca-$index" -file "$certificate" \
                -keystore "$trust_dir/ca.p12" -storetype PKCS12 \
                -storepass:env DB_TRUSTSTORE_PASSWORD >/dev/null 2>&1 ||
                fail "The Aiven CA certificate could not be imported. Check that it is a valid PEM certificate."
        done
    fi
    export DB_TRUSTSTORE_URL="file://$trust_dir/ca.p12"
    unset DB_TRUSTSTORE_BASE64 DB_CA_CERT_BASE64
fi

profiles=",${SPRING_PROFILES_ACTIVE:-${APP_PROFILE:-dev}},"
case "${1:-}" in
    -version|--version|-help|--help) ;;
    *)
        case "$profiles" in
            *,prod,*)
                [ -n "${DB_TRUSTSTORE_URL:-}" ] ||
                    fail "Aiven TLS certificate missing. Upload its CA as /etc/secrets/ca.pem in Render Secret Files, or set DB_CA_CERT_PATH or DB_CA_CERT_BASE64."
                [ -n "${DB_TRUSTSTORE_PASSWORD:-}" ] ||
                    fail "An existing PKCS12 truststore requires DB_TRUSTSTORE_PASSWORD. Upload ca.pem instead to generate the store automatically."
                ;;
        esac
        ;;
esac
exec java "$@"
