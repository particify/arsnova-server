package de.thm.arsnova.service.authservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties
class AuthServiceProperties (
        var server: Server? = null,
        var rabbitmq: Rabbitmq? = null
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
