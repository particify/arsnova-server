package de.thm.arsnova.service.authservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties
class AuthServiceProperties (
        var server: Server? = null,
        var rabbitmq: Rabbitmq? = null,
        var spring: Spring? = null
)

data class Server (
        var port: Int = 0
)

data class Rabbitmq (
        var host: String = "",
        var port: Int = 0,
        var username: String = "",
        var password: String = "",
        var virtualHost: String = ""
)

data class Spring (
        var datasource: Datasource? = null,
        var hibernate: Hibernate? = null
)

data class Datasource (
        var url: String = "",
        var driverClassName: String = "",
        var platform: String = "",
        var username: String = "",
        var password: String = ""
)

data class Jpa (
        var hibernate: Hibernate? = null
)

data class Hibernate (
        var ddlAuto: String = ""
)
