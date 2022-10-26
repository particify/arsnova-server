#/bin/sh

SERVICE=$1
OLDVER=$2
NEWVER=$3
VOLUME=particify_postgresql_data_$SERVICE
IMAGE=tianon/postgres-upgrade
ROOTDIR=/var/lib/postgresql
POSTGRES_USER=arsnova$SERVICE
POSTGRES_PASSWORD=arsnova$SERVICE

if [ -z "$SERVICE" ] || [ -z "$OLDVER" ] || [ -z "$NEWVER" ]; then
  echo "Missing argument. Usage: ./`basename $0` <service name> <old PG version> <new PG version>"
  exit 1
fi
echo Requested migration from version $OLDVER to $NEWVER.
CURVER=`docker run -v $VOLUME:/data alpine cat /data/PG_VERSION | tr -d '\n\r'`
echo Current version of data: $CURVER
if [ "$CURVER" != "$OLDVER" ]; then
  echo ERROR: Migration cannot be applied to current version of database.
  exit 1
fi
INVALID_VERSION_RANGE=`expr $OLDVER != $NEWVER - 1`
if [ $INVALID_VERSION_RANGE = 1 ]; then
  echo ERROR: Can only migrate to the next major version.
  exit 1
fi

read -p "Backup the database volumes before continuing. Continue? (y/N) " READ_CONTINUE
if [ "$READ_CONTINUE" != y ] && [ "$READ_CONTINUE" != Y ]; then
  echo Migration cancelled.
  exit 0
fi

set -e
echo Adjusting directory layout for migration...
docker run --rm -v $VOLUME:$ROOTDIR -w $ROOTDIR alpine sh -c "export src=\`echo *\` && mkdir -p $OLDVER/data && echo mv \$src $OLDVER/data && mv \$src $OLDVER/data && mkdir -p $NEWVER/data"
echo Running migration...
docker run --rm -v $VOLUME:$ROOTDIR -e PGUSER=$POSTGRES_USER -e POSTGRES_INITDB_ARGS="-U $POSTGRES_USER" $IMAGE:$OLDVER-to-$NEWVER --link
echo Restoring directory layout...
docker run --rm -v $VOLUME:$ROOTDIR -w $ROOTDIR alpine sh -c "mv $NEWVER/data/* ./ && rm -rf $OLDVER $NEWVER && printf \"\\nhost all all all md5\\n\" >> pg_hba.conf"
echo Migration completed.
