#!/bin/sh
DEST=credentials
KEY_FILE=$DEST/rp.key
CSR_FILE=$DEST/rp.csr
CRT_FILE=$DEST/rp.crt
KEYCLOAK_REALM_FILE=../.docker/keycloak/import/dev-realm.json
cd "$(dirname "$0")"
mkdir -p "$DEST"
openssl genrsa -out "$KEY_FILE" 4096
openssl req -new -key "$KEY_FILE" -out "$CSR_FILE" -subj "/CN=example.com"
openssl x509 -req -days 3650 -in "$CSR_FILE" -signkey "$KEY_FILE" -out "$CRT_FILE"
export SAML_SIGNING_CERTIFICATE="$(openssl x509 -in "$DEST/rp.crt" -outform der | base64 -w 0)"
cat "$KEYCLOAK_REALM_FILE.template" | envsubst '$SAML_SIGNING_CERTIFICATE' > "$KEYCLOAK_REALM_FILE"
