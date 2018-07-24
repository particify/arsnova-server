# Roadmap

## 4.0

* Remove API v2 compatibility layer


## 3.1

TBD


## 3.0

Version 3.0 is a rewrite of large parts of the code base with the goal of improving its maintainability.
A new streamlined REST API is developed while the legacy API is still supported through a compatibility layer.


### Beta 2

## Deployment & Operations

* Implement a monitoring endpoint for Prometheus


## Documentation

* Create/update developer documentation
	* REST API
	* Architecture
	* Coding guidelines
* Update installation and upgrade guide


## QA

* Review critical issues detected by static code analysis


### Beta 1

## General architecture

* Implement data validation for entities
* Use better caching implementation (Ehcache, etc.)
* Review and complete caching (annotations, keys, etc.)
* Review and complete authorization handling
* Review event system and unify emitting of events for CRUD operations
	* https://spring.io/blog/2015/02/11/better-application-events-in-spring-framework-4-2
* Review and improve error handling
	* Exceptions
	* HTTP responses
	* Logging
* Use modern logging framework (SLF4J with Logback instead of Log4j)


## REST API

* Adjust handling of non-persistent entities for API v3
* Reimplement export and import


## QA

* Increase code coverage for testing
* Implement performance tests (Gattling)
* Ensure Java 11 (LTS) compatibility
* Review blocker issues detected by static code analysis


### Alpha

## General architecture

* Minimize code duplication
	* Use generic classes for entity related controllers, services and data repositories
* Prefer constructor injection for required dependencies
	* Use setter injection for optional dependencies
	* Avoid field injection (it's fine in test classes)
* Use Jackson's JsonViews to define property visibility for API and persistence
* Refactor entities
	* Introduce abstract Entity class with common properties
	* Improve nomenclature for class and property naming
		* Avoid use-case specific names (such as "Peer Instruction" for multiple rounds)
		* Session -> Room
		* Lecture Question -> Content
		* Audience Question -> Comment
		* Learning Progress -> Score
		* Question Type -> Content Format
	* Do not reuse one entity type for multiple domain types
		* `Question` -> split up into `Content`, `Comment`
		* `Answer` -> split up into `Answer`, `AnswerStatistics`
	* Use subclasses to implement different formats for Content and Answer


## Persistence

* Abstract and split up persistence layer with help of Spring Data interfaces
* Get rid of JSON-lib (buggy and unmaintained) and use Jackson for all JSON (de-)serialization
* Use Ektorp instead of CouchDB4J (buggy and unmaintained) for CouchDB handling
* Implement view creation and data migration in Java (replace Setup Tool used for 2.x)
* Merge redundant database views
* Avoid emitting full docs and use `include_docs` instead


## REST API

* Implement support for handling multiple API versions
* Implement API v3
	* Stateless: No more `/whoami` or `/myxyz` routes
	* Use JWT instead of cookie-based sessions for authentication
	* Use generic routes for CRUD
		* Implement HTTP PATCH to update properties
		* No more `/<entity>/disablexyz`
	* Implement a `/<entity type>/find` endpoint
	* Provide statistics with non-aggregated combinations of answer choices
	* Implement customizable content groups
			* They replace predefined content variants
			* Relations are stored as part of a room
			* They support auto sort (v2 behavior) and user defined ordering
* Implement API live migration layer v2 <-> v3
	* v2 controllers convert between v2 and v3 entity types
	* Internally (service and persistence layer) only the v3 entity type is used
