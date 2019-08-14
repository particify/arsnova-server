# ARSnova installation

ARSnova has evolved to a distributed system that needs a lot of components running and quiet some prequirements before it can be run.

This document will guide you through the process of setting up a **local** development environment and since it is under heavy development, some systems will change. I'll try and keep this one up to date, if anything isn't clear or doesn't work, please use the [issue system](https://git.thm.de/arsnova/arsnova-comment-service/issues/new?issue).

Please note that this aims at debian-based systems. I can't give support for anything besides that.

## prerequirements

Dependencies:
- maven
- java 8
- node 8
- erlang (covered in the couchdb install instructions)
- curl
- git (obviously...)

### couchdb

* [Installation instruction for linux](https://docs.couchdb.org/en/stable/install/unix.html)
* [Getting Started](http://guide.couchdb.org/draft/tour.html)

todos:
- create database

notes:
- For local development, the admin account that's created with the installation can be used for ARSnova. Remember the password
- Create a new db for the system (see Getting Started guide)

### postgresql

* [Installation instructions for linux](http://postgresguide.com/setup/install.html)
* [Getting Started](https://wiki.debian.org/PostgreSql)

todos:
- enter psql console
- create database: `create database arsnovacomment;`
- create account for arsnova: `create user arsnovacomment with encrypted password 'arsnovacomment';`
- grant all privilegs to the new user on the new database: `grant all privileges on database arsnovacomment to arsnovacomment;`

### rabbitmq

* [Installation instructions for debian](https://www.rabbitmq.com/install-debian.html)
* [Infos on the STOMP plugin](https://rabbitmq.docs.pivotal.io/36/rabbit-web-docs/stomp.html)

todos:
- create user: `rabbitmqctl add_user arsnova arsnova`
- give user admin privileges: `rabbitmqctl set_user_tags arsnova administrator`
- give user all permissions: `rabbitmqctl set_permissions -p / arsnova ".*" ".*" ".*"`
- enable the STOMP plugin: `rabbitmq-plugins enable rabbitmq_stomp`
- [optional] enable web interface: `rabbitmq-plugins enable rabbitmq_management`

-----------------
## general
- create config directory `mkdir /etc/arsnova`


## core

* repo: `https://git.thm.de/arsnova/arsnova-backend`
* branch: `master` 

todos:
- copy config `cp src/main/resources/defaults.yml /etc/arsnova/application.yml`
- change config:
  - change the password in the section couchdb accordingly
  - change the db name in the section couchdb accordingly
  - change cors-origins to `*`
  - change mmessage-broker relay enabled to `true`


## comment service

* repo: `https://git.thm.de/arsnova/arsnova-comment-service`
* branch: `master`

todos:
- copy config `cp src/main/resources/arsnova.comment.properties.example /etc/arsnova/arsnova.comment.properties`
- change config:
  - change jdbc string or any other param if you didn't use the suggested names for dbs and accounts


## lite

* repo: `https://git.thm.de/arsnova/arsnova-lite`
* branch: `master`

todos:
- install dependencies: `npm install`

--------------

# Running the system

- ensure postgres and rabbitmq are running
- core:
  - `mvn jetty:run -D arsnova.config.dir=/etc/arsnova`
- comment-service:
  - `mvn spring-boot:run`
- lite:
  - `npm start`

