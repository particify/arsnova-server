# Caching

Please read about Spring Framework's [Cache Abstraction](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/cache.html) first.


## What should get cached?

The short answer: All data that is written once and read multiple times. In ARSnova, there is an inherent `1:n` relationship between teachers and students. This makes everything the teacher creates a candidate for caching. But there are more opportunities, like students' answers which are mostly written once and cannot be changed afterwards. Be aware though that in this case, once a new answer comes in, the cache has to be invalidated. With many students answering questions at the same time the effects of caching go away since the cache is invalidated all the time.

While caching provides an opportunity to greatly speed up the execution of various requests, it does come with a price: You have to think of all cases were the cached data might become stale.


## How to design your objects

Caching should only be used with domain objects, where the `hashCode` and `equals` methods are provided. This makes it easy to update or delete cache entries. As you recall from the documentation, cache keys are based on a method's parameters. If you use base objects like `String` or `Integer`, you will have to manually provide a key through the Spring Expression Language (SpEL). As you can see from the following example, such keys can become quite complicated:

```java
@Cacheable(value = "notverycacheable", key = "#p0.concat('-').concat(#p1).concat('-').concat(#p2)")
public ResultObject notVeryCacheable(String sessionId, String questionVariant, String subject) { ... }
```

Therefore, you should always work with domain objects like `Session`, `Content`, or even your own, newly defined objects:

```java
@Cacheable("verycacheable")
public ResultObject veryCacheable(Session session) { ... }
```

Be aware though that you need to carefully choose the fields which should be part of the `equals`/`hashCode`: In case of CouchDB, for example, it is not a good idea to use a document's `rev` field. Every time a document is updated, it gets a new `rev` which will make it _unequal_ to all its previous versions, making cache updates using `@CachePut` impossible.

[ARSnova's event system](https://github.com/thm-projects/arsnova-backend/wiki/Event-System) provides a useful way for fine-grained cache updates because the events contain all relevant domain objects. If you need to clear or update a cache based on one of ARSnova's events, you can use the `CacheBuster` class to add your annotations.


## Issues

Caching requires the use of Spring Proxies. This means that methods invoked using `this` ignore all caching annotations! They only work across object boundaries because Spring is only able to intercept calls if they are going through a Spring Proxy. This could only be solved using AOP, but we have no intention to support this in the near future.

There is one exception: Since the `databaseDao` bean needs to call several methods on the same object, we implemented a workaround that allows access to the bean's proxy. When `getDatabaseDao()` is called within the bean, its proxy is returned that should be used instead of `this`.

One last word of caution: Your code should not rely on the cache's existence, and you should keep expensive calls to a minimum: Do not hit the database multiple times even though you think further calls are served by the cache.


## List of cache entries and associated keys

Here is a list of all caches, their keys, and a short description.

Cache name | Key | Description
-----------|-----|------------
`contentlists`| database id of session | Contains all contents for the specified session irrespective of their variant.
`lecturecontentlists` | database id of session | Contains all "lecture" variant contents for the specified session.
`preparationcontentlists` | database id of session | Contains all "preparation" variant contents for the specified session.
`flashcardcontentlists` | database id of session | Contains all "flashcard" variant contents for the specified session.
`contents` | `Content` entity | Contains single content objects.
`contents` | database id of content | Although it shares the name of the previously mentioned cache, it is in essence a different cache because the keys are different. This means that the same `Content` object might be associated with two different keys.
`answerlists`| database id of content | Contains single answer objects.
`score` | `Session` entity | Contains `CourseScore` objects to calculate the score values for the specified session.
`sessions` | keyword of session | Contains sessions identified by their keywords.
`sessions` | database id of session | Although it shares the name of the previously mentioned cache, it is in essence a different cache because the keys are different. This means that the same `Session` object might be associated with two different keys.
`statistics` | -- | Contains a single, global statistics object.
