package de.thm.arsnova.service.httpgateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConfigurationProperties
@ConstructorBinding
data class HttpGatewayProperties(
        val security: Security,
        val httpClient: HttpClient,
        val routing: Routing
)

data class Security(
        val jwt: Jwt
)

data class Jwt(
        val publicSecret: String,
        val internalSecret: String,
        val serverId: String,
        val validityPeriod: Duration
)

data class HttpClient(
        val authService: String,
        val commentService: String,
        val core: String
)

data class Routing(
        val endpoints: Endpoints
)

data class Endpoints(
        val core: String,
        val commentService: String,
        val roomaccessService: String,
        val formattingService: String
)
