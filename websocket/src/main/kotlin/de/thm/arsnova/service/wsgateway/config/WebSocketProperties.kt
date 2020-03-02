package de.thm.arsnova.service.wsgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties
class WebSocketProperties (
	var serverPort: Int = 0,
	var messagingPrefix: String = "",
	var stomp: Stomp? = null
)
data class Relay (
	var host: String = "",
	var port: Int = 0,
	var user: String = "",
	var password: String = ""
)
data class Stomp (
	var relay: Relay? = null,
	var destinationPrefix: Array<String> = emptyArray(),
	var userRegistryBroadcast: String = "",
	var userDestinationBroadcast: String = ""
)
