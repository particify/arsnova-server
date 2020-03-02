package de.thm.arsnova.service.wsgateway.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableConfigurationProperties(WebSocketProperties::class)
@EnableWebSocketMessageBroker
class WebSocketConfig(
	private var webSocketProperties: WebSocketProperties
) : WebSocketMessageBrokerConfigurer {
	private val logger = LoggerFactory.getLogger(javaClass)


	override fun configureMessageBroker(config: MessageBrokerRegistry) {
		config
			.setApplicationDestinationPrefixes(webSocketProperties.messagingPrefix)
			.enableStompBrokerRelay(*webSocketProperties.stomp?.destinationPrefix.orEmpty())
			.setUserRegistryBroadcast(webSocketProperties.stomp?.userRegistryBroadcast.orEmpty())
			.setUserDestinationBroadcast(webSocketProperties.stomp?.userDestinationBroadcast.orEmpty())
			.setRelayHost(webSocketProperties.stomp?.relay?.host.orEmpty())
			.setRelayPort(webSocketProperties.stomp?.relay?.port!!)
			.setClientLogin(webSocketProperties.stomp?.relay?.user.orEmpty())
			.setClientPasscode(webSocketProperties.stomp?.relay?.password.orEmpty())
	}

	override fun registerStompEndpoints(registry: StompEndpointRegistry) {
		registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS()
	}

}
