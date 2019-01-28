# Changelog

## 2.6.3
Bug fixes:
* The backend now correctly responds with 4xx error codes instead of 500 to less
  common errors caused by bad requests from the client-side.

Additional changes:
* Libraries have been upgraded to fix potential bugs.

## 2.5.9
This is a maintenance release which only brings libraries up to date to fix
potential bugs.

## 2.6.2
This is a maintenance release which only brings libraries up to date to fix
potential bugs.

## 2.5.8
This is a maintenance release which only brings libraries up to date to fix
potential bugs.

## 2.6.1
Bug fixes:
* Fixed exception at startup caused by missing migration document.
* Fixed session export to correctly include all answers.

Additional changes:
* Libraries have been upgraded to fix potential bugs.

## 2.6
Features:
* Experimental support for CouchDB 2 has been added. Note: The data migration
  script is not compatible with CouchDB 2 and has to be run before an upgrade.

Improvements:
* Error handling and logging has been improved. It should now be easier to find
  the cause of problems. API error responses now contain the name of the
  `Exception` which caused the error. Further details for debugging purposes can
  be enabled with the new `api.expose-exception-messages` setting (Do NOT
  enable in production environments!).
* Updated OAuth handling to restore compatibility with 3rd-party login services.

Bug fixes:
* Fixed multiple bugs caused by incorrect type handling in the database layer.
* Fixed XFO header check behind reverse proxy (used by clients when embedding
  external websites).
* Fixed rounding error in learning progress calculation.
* Fixed `security.cors.origins` setting.
* Fixed import of data from older versions.

Security:
* Fixed DoS vulnerability in authentication handling behind reverse proxy.

Configuration changes:  
Minor changes to the web server and Tomcat proxy configuration are required
(see [installation guide](src/site/markdown/installation.md)).

**This version is brought to you by:**  
Project management: Klaus Quibeldey-Cirkel  
Lead programming: Daniel Gerhardt, Tom "tekay" Käsler  
Contributions: Marius Renner, Paul-Christian Volkmer  
Sponsoring: [AG QLS](https://www.thm.de/site/en/hochschule/service/ag-qls.html),
[HMWK](https://wissenschaft.hessen.de/wissenschaft/it-neue-medien/kompetenznetz-e-learning-hessen)


## 2.5.7
This is a maintenance release which only brings libraries up to date to fix
potential bugs.

## 2.5.6
This is a maintenance release which only brings libraries up to date to fix
potential bugs.

## 2.5.5
This is a maintenance release which only brings libraries up to date to fix
potential bugs.

## 2.5.4
This is a maintenance release which only brings libraries up to date to fix
potential bugs.

## 2.5.3
This is a maintenance release which only brings libraries up to date to fix
potential bugs.

## 2.5.2
This is a maintenance release which only brings libraries up to date to fix
potential bugs.

## 2.5.1
This release fixes a performance issue on session creation affecting large
installations.

Bug fixes:
* Session import works again.

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.4.3
This release fixes a performance issue on session creation affecting large
installations.

Bug fixes:
* WebSocket communication now works correctly for course sessions.
  (only affects installations using the LMS connector)
* The configuration parameter `security.facebook.allowed-roles` is now
  respected.

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.5
Major features:
* Administration API: New endpoints have been added which are accessible by
  users defined by `security.admin-accounts`.
* Evaluation of free text answers
* Proxy support for WebSocket connections: It is now possible to use the same
  port for standard HTTP requests and WebSocket connections. Additionally, it is
  no longer necessary to setup a Java key store for TLS if a proxy is used.
* Auto-deletion of inactive (not activated) users and guest sessions

Minor features and changes:
* Caching improvements
* New use case including only comments
* Export of questions to arsnova.click format
* Export/import of flashcards to/from arsnova.cards format
* Flashcards are now handled separately from questions

Configuration changes:
* `socketio.ip` has been replaced by `socketio.bind-address`
* `security.ssl` has been removed. `security.keystore` and `security.storepass`
  have been replaced by `socketio.ssl.jks-file` and `socketio.ssl.jks-password`.
* New setting: `socketio.proxy-path`
* The default port for WebSocket connections has been changed to `8090`

With this release we have completely overhauled our [documentation](README.md).
Additionally, we now provide
[Docker images](https://github.com/thm-projects/arsnova-docker/).

**This version is brought to you by:**  
Project management: Klaus Quibeldey-Cirkel  
Lead programming: Andreas Gärtner, Daniel Gerhardt, Tom "tekay" Käsler  
Contributions: Robin Drangmeister, Dennis Schönhof  
Sponsoring: [AG QLS](https://www.thm.de/site/en/hochschule/service/ag-qls.html),
[HMWK](https://wissenschaft.hessen.de/wissenschaft/it-neue-medien/kompetenznetz-e-learning-hessen)


## 2.4.2
This release fixes a minor security vulnerability which allowed an attacker to
remove a MotD from a session without being the creator.

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.3.4
This release fixes a minor security vulnerability which allowed an attacker to
remove a MotD from a session without being the creator.

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.4.1
This release fixes a security vulnerability caused by the CORS implementation.
Origins allowed for CORS can now be set in the configuration via
`security.cors.origins`. (Reported by Rainer Rillke at Wikimedia)

Additionally, authentication via disabled services is now entirely blocked to
fix a security vulnerability allowing guest access despite the setting
`security.guest.enabled=false`. (Reported by Rainer Rillke at Wikimedia)

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.3.3
This release fixes a security vulnerability caused by the CORS implementation.
Origins allowed for CORS can now be set in the configuration via
`security.cors.origins`. (Reported by Rainer Rillke at Wikimedia)

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.2.2
This release fixes a security vulnerability caused by the CORS implementation.
Origins allowed for CORS can now be set in the configuration via
`security.cors.origins`. (Reported by Rainer Rillke at Wikimedia)

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.1.2
This release fixes a security vulnerability caused by the CORS implementation.
Support for cross-origin requests has been removed. Use ARSnova version 2.2 or
newer for proper CORS. (Reported by Rainer Rillke at Wikimedia)

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.0.4
This release fixes a security vulnerability caused by the CORS implementation.
Support for cross-origin requests has been removed. Use ARSnova version 2.2 or
newer for proper CORS. (Reported by Rainer Rillke at Wikimedia)

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.4
Major features:
* Support for new use case and feature settings has been added.

Minor features and changes:
* User content is assigned to a single account regardless of case used at
  login/registration for database authentication. For LDAP authentication the
  UID attribute is requested from the server instead of relying on the user's
  input to ensure correct assignment.
* New API endpoints have been added to reduce requests on session imports.
* Session use case and feature settings are now included in exports and imports.
* Authentication providers can now be enabled separately for students and
  lecturers.
* A new suspended votes offset setting has been added.
* JSON export and import now include session info and feature settings.

Bug fixes:
* Deleted sessions are now correctly evicted from cache.
* Answer count calculation for free text questions has been fixed.

**This version is brought to you by:**  
Project management: Klaus Quibeldey-Cirkel  
Lead programming: Andreas Gärtner, Daniel Gerhardt, Tom "tekay" Käsler  
Contributions: Paul-Christian Volkmer  
Sponsoring: [AG QLS](https://www.thm.de/site/en/hochschule/service/ag-qls.html),
[HMWK](https://wissenschaft.hessen.de/wissenschaft/it-neue-medien/kompetenznetz-e-learning-hessen)


## 2.3.2
This release fixes a security vulnerability in the account management API. It is
highly recommended to upgrade if you are using database authentication.

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.2.1
This release fixes a security vulnerability in the account management API. It is
highly recommended to upgrade if you are using database authentication.

Additional bug fixes:
* The `security.authentication.login-try-limit` setting now works as intended.

## 2.1.1
This release fixes a security vulnerability in the account management API. It is
highly recommended to upgrade if you are using database authentication.

Additional changes:
* Libraries have been upgraded to fix potential bugs

## 2.0.3
This release fixes a security vulnerability in the account management API. It is
highly recommended to upgrade if you are using database authentication.

Additional changes:
* Libraries have been upgraded to fix potential bugs
* Some unnecessary log warnings for Websocket communication are filtered

## 2.3.1
Bug fixes:
* Case-insensitive user IDs are now correctly handled for LDAP authentication.
* LDAP authentication does no longer request unnecessary user attributes.

## 2.3
Major features:
* Improved LDAP authentication support: Additional settings for LDAP search and
  a privileged LDAP user have been added.
* Usernames for admin accounts can now be set up in the configuration file.
  These accounts are privileged to create global "Messages of the Day".
  Additional privileges might be added for them in future releases.
* Splash screen settings have been added to override the frontend theme's
  defaults.
* The API has been extended to support features introduced with ARSnova Mobile
  2.3.

Minor features and changes:
* Markdown formatting, learning progress, student's own questions and the
  question format flashcard are now active by default and can no longer be
  disabled for the whole ARSnova installation.

Bug fixes:
* The `security.authentication.login-try-limit` setting now works as intended.

Changes for developers:
* API documentation is now exposed in Swagger format.
* Startup time of Jetty has been significantly reduced.
* Version information is now saved with builds and exposed by the API.

**This version is brought to you by:**  
Project management: Klaus Quibeldey-Cirkel  
Lead programming: Andreas Gärtner, Daniel Gerhardt, Tom "tekay" Käsler,
Christoph Thelen  
Contributions: Eduard Ellert, Tjark Wilhelm Hoeck, Mohamed Sami Jarmoud, Stefan
Schmeißer, Paul-Christian Volkmer  
Sponsoring: [AG QLS](https://www.thm.de/site/en/hochschule/service/ag-qls.html),
[HMWK](https://wissenschaft.hessen.de/wissenschaft/it-neue-medien/kompetenznetz-e-learning-hessen)  


## 2.2
This release massively improves performance of ARSnova and contains a critical
bugfix so it is highly recommended to upgrade. Because of the newly introduced
caching method, it might be necessary to increase the Java memory limit for
servlet containers.

Major features:
* Performance improvements: Database queries are now cached by the backend.
  Answers are written to the database in batches.
* Pagination support: The range of results can now be limited.
* The API has been extended to support features introduced with ARSnova Mobile
  2.2.

Bug fixes:
* User content consisting of JSON could not be loaded and rendered connected
  data unloadable as well.

**This version is brought to you by:**  
Project management: Klaus Quibeldey-Cirkel  
Lead programming: Andreas Gärtner, Daniel Gerhardt, Christoph Thelen  
Contributions: Dominik Hikade, Tom Käsler, Maximilian Klingelhöfer,
Michael Sann, Jan Sladek, Katharina Staden  
Sponsoring: [AG QLS](https://www.thm.de/site/en/hochschule/service/ag-qls.html),
[HMWK](https://wissenschaft.hessen.de/wissenschaft/it-neue-medien/kompetenznetz-e-learning-hessen)  


## 2.1
Major features:
* Public Pool (experimental): It is now possible to share sessions with other
  users in a pool of public sessions. Other users can create their own copies of
  shared sessions. This feature can be enabled in the arsnova.properties
  configuration.

Minor features and changes:
* Adjustments to correctly handle requests for imports from the frontend.
* Some communication between the frontend and backend has been optimized for
  improved performance.
* Additional configuration parameters for tracking, session export and import, a
  demo session and a blog URL have been introduced.

**This version is brought to you by:**  
Project management: Klaus Quibeldey-Cirkel  
Lead programming: Andreas Gärtner, Daniel Gerhardt, Christoph Thelen  
Contributions: Felix Schmidt, Artjom Siebert, Daniel Vogel  
Sponsoring: [AG QLS](https://www.thm.de/site/en/hochschule/service/ag-qls.html),
[HMWK](https://wissenschaft.hessen.de/wissenschaft/it-neue-medien/kompetenznetz-e-learning-hessen)  


## 2.0.2
This release updates dependencies. The updated library for Socket.IO support
fixes memory leaks and disables SSL 3.0 support (POODLE vulnerability).

## 2.0.1
This release introduces the following changes:
* Updates dependency for Socket.IO support to fix memory leaks
* Usernames for student's questions and free text answers are no longer exposed
  by API responses

## 2.0.0
ARSnova 2.0 has been in development for more than two years. Further releases
can be expected much more frequently.

This is actually the first major release of ARSnova Backend. It is called 2.0 to
feature API compatibility with the simultaneously released ARSnova Mobile 2.0.

**This version is brought to you by:**  
Project management: Klaus Quibeldey-Cirkel  
Lead programming: Andreas Gärtner, Daniel Gerhardt, Christoph Thelen,
Paul-Christian Volkmer  
Contributions: Sören Gutzeit, Julian Hochstetter, Jan Kammer, Daniel Knapp,
Felix Schmidt, Artjom Siebert, Daniel Vogel  
Testing & Feedback: Kevin Atkins, Kathrin Jäger  
Sponsoring: [AG QLS](https://www.thm.de/site/en/hochschule/service/ag-qls.html),
[HMWK](https://wissenschaft.hessen.de/wissenschaft/it-neue-medien/kompetenznetz-e-learning-hessen),
[@LLZ](http://llz.uni-halle.de/)  
