# Event System

ARSnova's event system allows objects to act upon changes to any of ARSnova's data structures. For example, if a new question is created, a `NewQuestionEvent` is sent out and any object that is interested can receive the question itself along with meta data such as the relevant `Session`. Among other use cases, the event is used to notify clients via Web Socket.

Events offer a way to dynamically update data without the need to poll the database for changes. For instance, if clients are interested in the number of currently available questions, the respective Bean could initialize the number using the database, while keeping it up to date using events.


## How to send events?

A class is able to send events by implementing the `ApplicationEventPublisherAware` interface. It consists of one method to inject an `ApplicationEventPublisher`, which can then be used to send the events like so:
```java
publisher.publishEvent(theEvent);
```
where `theEvent` is an object of type `ApplicationEvent`. For ARSnova, the base class `ArsnovaEvent` should be used instead. All of ARSnova's internal events are subtypes of `ArsnovaEvent`.

_Note_: Events are sent and received on the same thread, i.e., it is a synchronous operation.


## How to receive events?

Events are received by implementing the `ApplicationListener<ArsnovaEvent>` interface. The associated method gets passed in a `ArsnovaEvent`, which is the base class of all of ARSnova's events. However, this type itself is not very useful. The real type can be revealed using double dispatch, which is the basis of the Visitor pattern. Therefore, the event should be forwarded to a class that implements the `ArsnovaEvent` interface. This could be the same class that received the event.

_Note_: If the class implementing the Visitor needs to have some of Spring's annotations on the event methods, like, for example, to cache some values using `@Cacheable`, the Listener and the Visitor must be different objects.


## How to create custom events?

Subclass either `ArsnovaEvent` or `RoomEvent`. The former is for generic events that are not tied to a specific room, while the latter is for cases where the event only makes sense in the context of a room.
