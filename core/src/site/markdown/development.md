# Development

## Preparations

Before you can get started developing ARSnova you need to make sure the following software is installed to build ARSnova Backend:

* Java 17 JDK
* Apache Maven 3.x

And additionally if you want to run ARSnova locally:

* Apache CouchDB 1.x
* Python 2.7

Next, you need to setup an ARSnova configuration file.
Create a copy of [defaults.yml](../../main/resources/config/defaults.yml) in a directory like `/etc/arsnova`, then rename
the copied file to `application.yml`. The directory is defined by the property `arsnova.config-dir` and needs to be
supplied to ARSnova during startup, see section "Running" below.

## Building

ARSnova Backend uses Maven for builds and dependency management.
You do not need to download any framework or library dependencies - Maven handles this for you.

You can create a web archive (`.war` file) by running a single command:

	$ mvn package


## Running

ARSnova builds are setup up to automatically download the Java Servlet container Jetty for development.
Run the following command to download the dependencies, and startup the backend with Jetty:

	$ mvn jetty:run -D arsnova.config-dir=/etc/arsnova

After a few seconds the ARSnova API will be accessible at <http://localhost:8080/>.

You can adjust the amount of debug logging by changing the log levels in [log4j-dev.properties](../../main/resources/log4j-dev.properties).
Additionally, you can enable exception messages in API responses by setting the boolean property `api.expose-error-messages` in `arsnova.properties`.


## Continuous Integration

Our code repositories are located on a [GitLab server](https://git.thm.de/arsnova) for internal development.
They are automatically mirrored to [GitHub](https://github.com/thm-projects) on code changes.

Apart from mirroring GitLab CI triggers various jobs to:

* check the code quality (static code analysis with SonarQube)
* build a web archive
* execute unit tests
* deploy to our staging/production servers

In addition to GitLab CI for our internal repositories, we use Travis CI which is able to run against merge requests on GitHub.
Travis CI only runs unit tests for the backend.

The current build status for the master branch:

* [![Build Status](https://travis-ci.org/thm-projects/arsnova-backend.svg?branch=master)](https://travis-ci.org/thm-projects/arsnova-backend) for ARSnova Backend
* [![Build Status](https://travis-ci.org/thm-projects/arsnova-mobile.svg?branch=master)](https://travis-ci.org/thm-projects/arsnova-mobile) for ARSnova Mobile


## Further Documentation

* [Roadmap](development/roadmap.md)
* [Caching](development/caching.md)
* [Event System](development/event-system.md)
