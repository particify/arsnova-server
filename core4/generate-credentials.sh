#!/bin/sh
set -e

DEST=credentials
KEYCLOAK_REALM_FILE=../.docker/keycloak/import/dev-realm.json
FORCE=false

for arg in "$@"; do
  case "$arg" in
    -f|--force)
      FORCE=true
      ;;
    *)
      echo "Usage: $(basename "$0") [-f|--force]" >&2
      exit 1
      ;;
  esac
done

# Keys are kept unless they are missing so that the realm data can be rendered again on its own:
# Keycloak imports the realm only if it does not exist yet, so new keys would leave an already
# imported realm with the old signing certificate.
generate_keypair() {
  key_file=$1
  csr_file=$2
  crt_file=$3
  subject=$4
  if [ -f "$key_file" ] && [ "$FORCE" != true ]; then
    echo "Keeping existing key $key_file, pass --force to replace it."
    return
  fi
  openssl genrsa -out "$key_file" 4096
  openssl req -new -key "$key_file" -out "$csr_file" -subj "$subject"
  openssl x509 -req -days 3650 -in "$csr_file" -signkey "$key_file" -out "$crt_file"
}

cd "$(dirname "$0")"
mkdir -p "$DEST"

generate_keypair "$DEST/rp.key" "$DEST/rp.csr" "$DEST/rp.crt" "/CN=example.com"
generate_keypair "$DEST/idp.key" "$DEST/idp.csr" "$DEST/idp.crt" "/CN=keycloak"

SAML_SP_SIGNING_CERTIFICATE="$(openssl x509 -in "$DEST/rp.crt" -outform der | base64 -w 0)"
export SAML_SP_SIGNING_CERTIFICATE
SAML_IDP_SIGNING_CERTIFICATE="$(openssl x509 -in "$DEST/idp.crt" -outform der | base64 -w 0)"
export SAML_IDP_SIGNING_CERTIFICATE
# Keycloak expects PKCS#8, which `openssl rsa` would not produce from a PKCS#1 key.
SAML_IDP_SIGNING_PRIVATE_KEY="$(openssl pkey -in "$DEST/idp.key" -outform der | base64 -w 0)"
export SAML_IDP_SIGNING_PRIVATE_KEY
envsubst '$SAML_IDP_SIGNING_CERTIFICATE $SAML_IDP_SIGNING_PRIVATE_KEY $SAML_SP_SIGNING_CERTIFICATE' \
  < "$KEYCLOAK_REALM_FILE.template" > "$KEYCLOAK_REALM_FILE"

cat <<'MESSAGE'

Rendered the realm data for Keycloak.
Keycloak skips the import if the realm already exists, so it needs to be deleted for the change to
take effect:

./pdk compose exec keycloak /opt/keycloak/bin/kcadm.sh delete realms/dev --no-config \
  --server http://localhost:8080 --realm master --user admin --password admin
./pdk compose restart keycloak
MESSAGE
