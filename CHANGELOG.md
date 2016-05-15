# Changelog

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

Bugfixes:
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
