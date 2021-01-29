# Development

## Preparations

Before you can get started developing ARSnova you need to make sure the following software is installed to build ARSnova Backend:

* OpenJDK 8/11 (JDK)
* Apache Maven 3.x

And additionally if you want to run ARSnova locally:

* Apache CouchDB 2 or later
* Python 3.2 or later
* [ARSnova 2 Setup Tool](https://gitlab.com/particify/dev/foss/arsnova-2-setup-tool)

Next, you need to setup an ARSnova configuration file.
Create a copy of [arsnova.properties.example](../../main/resources/arsnova.properties.example) at `/etc/arsnova/arsnova.properties`.
Afterwards, initialize the database by running the `tool.py` python script from the Setup Tool.


## Building

ARSnova Backend uses Maven for builds and dependency management.
You do not need to download any framework or library dependencies - Maven handles this for you.

You can create a web archive (`.war` file) by running a single command:

	$ mvn package


## Running

ARSnova builds are setup up to automatically download the Java Servlet container Jetty for development.
Run the following command to download the dependencies, and startup the backend with Jetty:

	$ mvn jetty:run

After a few seconds the ARSnova API will be accessible at <http://localhost:8080/>.

You can adjust the amount of debug logging by changing the log levels in [logback.xml](../../main/resources/logback.xml).
Additionally, you can enable exception messages in API responses by setting the boolean property `api.expose-error-messages` in `arsnova.properties`.


## Continuous Integration

Our [code repositories](https://gitlab.com/particify/dev/foss) are located at GitLab.

GitLab CI triggers various jobs for new commits to:

* check the code quality
* build packages (web archives)
* execute unit tests

The current build status for the master branch:

* [![Build Status](https://gitlab.com/particify/dev/foss/arsnova-backend/badges/2.x/pipeline.svg)](https://gitlab.com/particify/dev/foss/arsnova-backend/-/pipelines?scope=branches&ref=2.x) for ARSnova Backend
* [![Build Status](https://gitlab.com/particify/dev/foss/arsnova-webclient-legacy/badges/2.x/pipeline.svg)](https://gitlab.com/particify/dev/foss/arsnova-webclient-legacy/-/pipelines?scope=branches&ref=2.x) for ARSnova Mobile


## Further Documentation

* [Caching](development/caching.md)
* [Event System](development/event-system.md)
