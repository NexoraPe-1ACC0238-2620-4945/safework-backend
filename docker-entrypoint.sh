#!/bin/sh
set -eu

# Render environment values are text. Decode a dedicated truststore locally
# instead of committing a binary certificate store or a password to the image.
if [ -n "${DB_TRUSTSTORE_BASE64:-}" ]; then
    if [ -n "${DB_TRUSTSTORE_URL:-}" ]; then
        echo "Configure DB_TRUSTSTORE_BASE64 or DB_TRUSTSTORE_URL, not both." >&2
        exit 64
    fi
    umask 077
    trust_dir="$(mktemp -d "${TMPDIR:-/tmp}/safework-trust.XXXXXX")"
    printf '%s' "$DB_TRUSTSTORE_BASE64" | base64 -d > "$trust_dir/ca.p12"
    export DB_TRUSTSTORE_URL="file://$trust_dir/ca.p12"
    unset DB_TRUSTSTORE_BASE64
fi

exec java "$@"
