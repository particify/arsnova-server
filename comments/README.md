# Setup

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
