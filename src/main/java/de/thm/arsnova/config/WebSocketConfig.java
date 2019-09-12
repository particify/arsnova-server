package de.thm.arsnova.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import de.thm.arsnova.config.properties.MessageBrokerProperties;
import de.thm.arsnova.config.properties.SecurityProperties;
import de.thm.arsnova.websocket.handler.AuthChannelInterceptorAdapter;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(MessageBrokerProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	private static final String MESSAGING_PREFIX = "/backend";
	private static final String[] DESTINATION_PREFIX = {"/exchange", "/topic", "/queue"};
	private static final String USER_REGISTRY_BROADCAST = "/topic/log-user-registry";
	private static final String USER_DESTINATION_BROADCAST = "/queue/log-unresolved-user";

	private final MessageBrokerProperties.Relay relayProperties;
	private final AuthChannelInterceptorAdapter authChannelInterceptorAdapter;
	private String[] corsOrigins;

	@Autowired
	public WebSocketConfig(
			final MessageBrokerProperties messageBrokerProperties,
			final SecurityProperties securityProperties,
			final AuthChannelInterceptorAdapter authChannelInterceptorAdapter) {
		this.relayProperties = messageBrokerProperties.getRelay();
		this.corsOrigins = securityProperties.getCorsOrigins().stream().toArray(String[]::new);
		this.authChannelInterceptorAdapter = authChannelInterceptorAdapter;
	}

	@Override
	public void configureMessageBroker(final MessageBrokerRegistry config) {
		config.setApplicationDestinationPrefixes(MESSAGING_PREFIX);

		if (relayProperties.isEnabled()) {
			config
					.enableStompBrokerRelay(DESTINATION_PREFIX)
					.setUserRegistryBroadcast(USER_REGISTRY_BROADCAST)
					.setUserDestinationBroadcast(USER_DESTINATION_BROADCAST)
					.setRelayHost(relayProperties.getHost())
					.setRelayPort(relayProperties.getPort())
					.setClientLogin(relayProperties.getUsername())
					.setClientPasscode(relayProperties.getPassword());
		} else {
			config.enableSimpleBroker(DESTINATION_PREFIX);
		}
	}

	@Override
	public void registerStompEndpoints(final StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOrigins(corsOrigins).withSockJS();
	}

	@Override
	public void configureClientInboundChannel(final ChannelRegistration registration) {
		registration.interceptors(authChannelInterceptorAdapter);
	}
}
