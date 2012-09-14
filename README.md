# ARSnova 2

This project really brings you *two* different versions of ARSnova: `arsnova-js` (ARSnova 2) and `arsnova-legacy-js` (ARSnova 1).

The first one is currently under heavy development and is not ready for production use. The second one is the tried-and-true ARSnova for your mobile device. However, `arsnova-legacy-js` will not receive any major updates and is nearing its end of life. It will be superseded by `arsnova-js`.

## Getting started

Both versions of ARSnova will be deployed alongside each other, so you get to choose which one you would like to use. By default, `arsnova-legacy-js` is served via `index.html` and optionally via `developer.html`. If you want to get your hands dirty, you should open `dojo-index.html` and try out the redesigned ARSnova 2. It will work on any major browser instead of being for Webkit browsers only.

## Deployment

You will need to do some configuration work upfront.

 * Add a new directory "arsnova" in /etc and create a copy of arsnova.properties.example named arsnova.properties in this directory.
 * Change settings to match your environment

## Server configuration

In order to build up a full featured server installation containing ARSnova2 and CouchDB you have to install at least the following services:
 * Apache Tomcat 7.0.29 (or newer)
 * Apache Webserver 2.2 or newer with buildin mod_proxy, mod_proxy_ajp and mod_proxy_http
 * Apache CouchDB
 
Make sure all services are installed. Next step is to configure the Apache Webserver. Find the configuration file or create a new one for use with a virtal host. This depends on your needs. At least you should have a configuration containing these settings:

<Location /couchdb/>
 ProxyPass http://127.0.0.1:5984/
 ProxyPassReverse http://127.0.0.1:5984/
</Location> 
<Location />
 ProxyPass ajp://127.0.0.1:8009/
 ProxyPassReverse ajp://127.0.0.1:8009/
</Location>

This will redirect all requests for "/couchdb/..." to your Apache CouchDB server, running on port 5984.
All other requests will be send to your Apache Tomcat servelt container, using AJP running on port 8009.

To enable the needed Apache Webserver simply type:

# a2enmod proxy
# a2enmod proxy_ajp
# a2enmod proxy_http

The configuration is ready for development usage. 

Finally you should (re)start all services. ARSnova2 is now listening on HTTP port 80.