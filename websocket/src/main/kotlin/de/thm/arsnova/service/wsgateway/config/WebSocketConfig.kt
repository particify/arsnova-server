package de.thm.arsnova.service.wsgateway.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.*

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
	private val logger = LoggerFactory.getLogger(javaClass)

	@Value("\${messaging.prefix}")
	private lateinit var messagingPrefix: String
	@Value("\${stomp.destination.prefix}")
	private lateinit var destinationPrefix: Array<String>
	@Value("\${stomp.relay.host}")
	private lateinit var relayHost: String
	@Value("\${stomp.relay.port}")
	private var relayPort: Int = 0
	@Value("\${stomp.relay.user}")
	private lateinit var relayUser: String
	@Value("\${stomp.relay.password}")
	private lateinit var relayPassword: String
	@Value("\${stomp.user.registry.broadcast}")
	private lateinit var userRegistryBroadcast: String
	@Value("\${stomp.user.destination.broadcast}")
	private lateinit var userDestinationBroadcast: String

	override fun configureMessageBroker(config: MessageBrokerRegistry) {
		config
			.setApplicationDestinationPrefixes(messagingPrefix)
			.enableStompBrokerRelay(*destinationPrefix)
			.setUserRegistryBroadcast(userRegistryBroadcast)
			.setUserDestinationBroadcast(userDestinationBroadcast)
			.setRelayHost(relayHost)
			.setRelayPort(relayPort)
			.setClientLogin(relayUser)
			.setClientPasscode(relayPassword)
	}

	override fun registerStompEndpoints(registry: StompEndpointRegistry) {
		registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS()
	}

}
