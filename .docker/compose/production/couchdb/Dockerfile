ARG IMAGE_BASETAG=3
FROM couchdb:${IMAGE_BASETAG}

COPY --chown=couchdb:couchdb custom.ini /opt/couchdb/etc/local.d/99-custom.ini
