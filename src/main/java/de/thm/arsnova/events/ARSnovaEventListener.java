package de.thm.arsnova.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import de.thm.arsnova.services.UserService;
import de.thm.arsnova.services.UserSessionService;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Component
public class ARSnovaEventListener implements ApplicationListener<ARSnovaEvent> {

	public static final Logger LOGGER = LoggerFactory.getLogger(ARSnovaEventListener.class);

	@Autowired
	private ARSnovaSocketIOServer socketIoServer;

	@Autowired
	private UserService userService;

	@Override
	public void onApplicationEvent(ARSnovaEvent event) {
		for( UserSessionService userSessionService : userService.getUserSessionServices().values() ) {
			if (userSessionService != null) {
				LOGGER.info(userSessionService.getUser().getUsername());
				userSessionService.sendEventViaWebSocket(socketIoServer, event);
			}
		}
	}
}
