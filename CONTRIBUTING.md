# Contributing

ARSnova needs you! If you are interested in helping, please review the guidelines found in our [mobile repository][mobile-repository].

[mobile-repository]: https://github.com/thm-projects/arsnova-mobile/blob/master/CONTRIBUTING.md

## Interactive Documentation of the REST API with Springfox(Swagger)

[Springfox](http://springfox.github.io/springfox/docs/current/) automated JSON API documentation for API's built with Spring and 
supports both version 1.2 and version 2.0 of the [Swagger Specification](https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#swagger-restful-api-documentation-specification).
In this case we use the version 2.0 of Swagger. 

In the [Swagger Annotation Overview](https://github.com/swagger-api/swagger-core/wiki/Annotations) the most frequently-used Annotions are listed and described. Please use them in case of creating new REST API endpoints. If you do so, the interactive API documentation will be created automatically by Springfox. In need of additional informations, please visit our [ARSnova Wiki entry](https://wiki.thm.de/ARSnova#Interaktive_Dokumentation_der_REST-API_mit_Springfox.28Swagger.29).

To see all REST APIs from ARSnova-Backend, you can use the follow Swagger Userinface URL: 
http://localhost:8080/swagger-ui.html
