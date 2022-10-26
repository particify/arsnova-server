# Setup

Install dependencies:
```
apt-get install libpostgresql-jdbc-java postgresql
```

Setup DB:

```
sudo -u postgres psql
create database arsnovaauth;
create user arsnovaauth with encrypted password 'arsnovaauth';
grant all privileges on database arsnovaauth to arsnovaauth;
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
