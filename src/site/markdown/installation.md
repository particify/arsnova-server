# Installation

This document describes the necessary steps to install ARSnova Backend.
If you are viewing this file from the repository, please make sure you are on the corresponding stable branch (e.g. `2.8`) for the target version.


## Requirements

### Operating System

While ARSnova should be able to run on any Linux distribution, we officially only support Debian and Ubuntu systems on which we test the installation procedure regularly.
We recommend to use the latest (LTS) versions of the distributions.

For Debian 9 you can enable the backports repository to install OpenJDK 11 and Tomcat 9:

	# grep -q '^deb .*stretch-backports' /etc/apt/sources.list || echo "deb http://deb.debian.org/debian stretch-backports main" >> /etc/apt/sources.list


### Hardware

While the hardware requirements highly depend on the number of simultaneous users, we suggest to provide at least 2 CPU cores and 4 GiB memory to the system running ARSnova.
This configuration would support up to 500 users.


## Configuration

You will need to do some configuration work upfront:
Create a copy of
[src/main/resources/arsnova.properties.example](../../main/resources/arsnova.properties.example)
at `/etc/arsnova/arsnova.properties`.
Then change the settings to match your environment.

You need to change at least the following configuration properties:

* `root-url`: The public URL (without backend path) at which ARSnova will be accessible to clients.
* `couchdb.user` and `couchdb.password`: These credentials will be used later when the database is setup.

*A note to Windows users*: our settings are based on Linux and Mac environments.
We do not officially support Windows, so you have to do some extra steps.
The property file's path is hard coded in
[src/main/webapp/WEB-INF/spring/spring-main.xml](../../main/webapp/WEB-INF/spring/spring-main.xml)
and in the "Setup Tool" (see "Database Setup").
You want to change the paths to make them match your environment.


## Server Software

In order to build up a full featured server installation you have to install at least the following software:

* OpenJDK 8/11 (JRE)
* Apache Tomcat 8 (or later)
* Apache CouchDB 1.x (1.2 or later recommended)
* One of the following webservers acting as a reverse proxy:
	* Nginx 1.3 (or later), 1.9.5 (or later) recommended for HTTP/2 support
	* Apache HTTP Server 2.4.5 (or later) with builtin modules `mod_proxy`, `mod_proxy_http`, `mod_proxy_wstunnel` and `mod_rewrite`

Additionally, you need Python 3.2 or later to run the "Setup Tool".
We further recommend installing the "Apache Portable Runtime (APR) based Native library for Tomcat" (libapr and libtcnative) for improved performance.

Most of this software can easily be installed on Linux systems using the distribution's package manager:
* Debian/Ubuntu: `# apt-get install libtcnative-1 nginx openjdk-11-jre tomcat9`

While running ARSnova without a reverse proxy is possible, we do not recommend to do so.
A reverse proxy significantly simplifies the setup of HTTPS and allows running Websocket connections over the default HTTP(S) port.


### CouchDB

If you are installing ARSnova for the first time and don't have any data from an older version that needs to be migrated,
you can follow CouchDB's guides to set up CouchDB 2 or later:
* [Installation](https://docs.couchdb.org/en/stable/install/unix.html)
* [Single Node Setup](https://docs.couchdb.org/en/stable/setup/single-node.html)

After CouchDB is installed successfully, [set up an admin user](https://docs.couchdb.org/en/stable/config/auth.html)
with the credentials set before for `couchdb.username` and `couchdb.password`.

We provide a Python script that will set up a new database for ARSnova.
This "Setup Tool" is located at [thm-projects/arsnova-setuptool](https://github.com/thm-projects/arsnova-setuptool).

To set up the database, run:

	$ ./tool.py

This will create the database along with all required view documents.

CouchDB is now usable for ARSnova, but there are still a few things that should be setup for performance reasons.
We recommend that you enable scheduled compactions of databases and their views.
Create a new file `99-compactions.ini` in the CouchDB configuration directory (usually located at `/opt/couchdb/etc/local.d`) with the following content:

	[compactions]
	_default = [{db_fragmentation, "70%"}, {view_fragmentation, "60%"}, {from, "01:00"}, {to, "05:00"}]

To make compaction effective, you need to reduce the number of revisions saved per document.
As long as CouchDB is not clustered, you can significantly reduce storage usage by setting a limit of 5.

	$ curl -X PUT -d "5" http://localhost:5984/arsnova/_revs_limit


### Java & Tomcat

To make sure ARSnova has access to the memory provided by the system, you need adjust the Java settings for Tomcat.
Increase the initial and maximum heap sizes by adding `-Xms3072m -Xmx3072m` to the Java start up parameters used by Tomcat.
On Debian-based systems this is done by appending

	JAVA_OPTS="$JAVA_OPTS -Xms3072m -Xmx3072m"

to `/etc/default/tomcat9`.
For most other distributions append this to `setenv.sh` in Tomcat's `bin` directory.
Adjust the values based on the memory available to the system, but make sure to reserve 1 GiB for the operating system, web server, and database system.

By default, Tomcat listens on a public interface.
If you follow our recommendation to use a reverse proxy, you should change the configuration so Tomcat only accepts requests from localhost.
Open `/etc/tomcat9/server.xml`, look for `<Connector port="8080" ...>`, and change it to:

	<Connector port="8080" address="127.0.0.1" ...>

Additionally, you need to add a
[Remote IP Valve](https://tomcat.apache.org/tomcat-9.0-doc/config/valve.html#Remote_IP_Valve)
to make Tomcat aware of the `X-Forwarded` headers injected by the reverse proxy.
Look for the `<Host name="localhost" ...>` section and add:

    <Valve className="org.apache.catalina.valves.RemoteIpValve"
        internalProxies="127\.0\.0\.1"
        remoteIpHeader="x-forwarded-for"
        protocolHeader="x-forwarded-proto" />

If the reverse proxy is not running locally, you need to adjust the regular expression for `internalProxies` accordingly.


### Web Application

The ARSnova Backend application is contained in a single file: the web archive (`.war file`).
You can download the latest version from our
[GitHub releases page](https://github.com/thm-projects/arsnova-backend/releases).

To deploy the backend on the Tomcat Servlet container, copy the file to Tomcat's webapp directory, and name it `api.war`.

Check that the application is deployed correctly by sending a HTTP request:

	$ curl -H "Accept: application/json" localhost:8080/api/

The backend should respond with version information.


### Nginx

Create a new site configuration file `arsnova` in `/etc/nginx/sites-available` based on the default site configuration.
Add the following lines to the `server` section of the file:

	location = / { return 301 <path to ARSnova frontend>; }
	location = /arsnova-config { proxy_pass http://localhost:8080/api/configuration/; }
	location /api {
		proxy_pass http://localhost:8080;
		proxy_set_header Host $host;
		proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
		proxy_set_header X-Forwarded-Proto $scheme;
	}
	location /socket.io/ {
		proxy_pass http://localhost:8090;
		proxy_http_version 1.1;
		proxy_set_header Upgrade $http_upgrade;
		proxy_set_header Connection "upgrade";
	}

Create a symbolic link in `/etc/nginx/sites-enabled` to the `arsnova` site configuration file.
Delete a link to the default site configuration if it exists.


### Apache HTTP Server

Create a new site configuration file `arsnova` in `/etc/apache2/sites-available` based on the default site configuration.
Add the following lines to the `VirtualHost` section of the file:

	RewriteEngine On
	RewriteRule ^/$ <path to ARSnova frontend> [R=301,L]
	RewriteRule ^/arsnova-config$ /api/configuration/ [PT,L]
	<Location /api/>
		ProxyPass http://localhost:8080/api/
		ProxyPassReverse http://localhost:8080/api/
		ProxyPreserveHost On
		RequestHeader set X-Forwarded-Proto %{REQUEST_SCHEME}e
	</Location>
	<Location /socket.io/>
		ProxyPass ws://localhost:8090/socket.io/
		ProxyPassReverse ws://localhost:8090/socket.io/
	</Location>

To enable the required Apache HTTP Server modules, simply type:

	# a2enmod headers
	# a2enmod proxy
	# a2enmod proxy_http
	# a2enmod proxy_wstunnel
	# a2enmod rewrite

At last, disable the default site configuration, and enable the newly created one:

	# a2dissite default
	# a2ensite arsnova


## Docker

If you want to run ARSnova in a containerized environment, you can use our Docker images.
Have a look at our [deployment instructions](https://github.com/thm-projects/arsnova-docker) for Docker.
