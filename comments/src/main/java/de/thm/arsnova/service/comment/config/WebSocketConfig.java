package de.thm.arsnova.service.comment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Value("${messaging.prefix}") private String messagingPrefix;
    @Value("${stomp.destination.prefix}") private String[] destinationPrefix;
    @Value("${stomp.relay.host}") private String relayHost;
    @Value("${stomp.relay.port}") private int relayPort;
    @Value("${stomp.relay.user}") private String relayUser;
    @Value("${stomp.relay.password}") private String relayPassword;
    @Value("${stomp.user.registry.broadcast}") private String userRegistryBroadcast;
    @Value("${stomp.user.destination.broadcast}") private String userDestinationBroadcast;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config
                .setApplicationDestinationPrefixes(messagingPrefix)
                .enableStompBrokerRelay(destinationPrefix)
                .setUserRegistryBroadcast(userRegistryBroadcast)
                .setUserDestinationBroadcast(userDestinationBroadcast)
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(relayUser)
                .setClientPasscode(relayPassword);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
    }

}