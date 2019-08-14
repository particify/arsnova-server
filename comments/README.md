# Setup

Install instructions for the arsnova systems are in the src/site/markdown/arsnova-installation.md.

Install dependencies:
```
apt-get install libpostgresql-jdbc-java postgresql
```

Setup DB:

```
sudo -u postgres psql
create database arsnovacomment;
create user arsnovacomment with encrypted password 'arsnovacomment';
grant all privileges on database arsnovacomment to arsnovacomment;
```


# Run
`mvn spring-boot:run`


### Logging
`-Dlogging.level.de.thm.arsnova=<debug-level>`

Possible options for debug-level:
- INFO
- TRACE
- DEBUG
- WARN
- ERROR
