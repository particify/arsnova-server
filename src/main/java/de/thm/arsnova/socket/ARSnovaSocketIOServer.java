package de.thm.arsnova.socket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.ClientOperations;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.parser.Packet;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.socket.message.Feedback;

public class ARSnovaSocketIOServer {

	@Autowired
	private ISessionService sessionService;	
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final Map<UUID, User> socketid2user = new ConcurrentHashMap<UUID, User>();
	
	private int portNumber;
	private boolean useSSL = false;
	private String keystore;
	private String storepass;
	private final Configuration config;
	private SocketIOServer server;


	public ARSnovaSocketIOServer() {
		config = new Configuration();
	}

	public void startServer() throws Exception {
		/**
		 * hack: listen to ipv4 adresses
		 */
		System.setProperty("java.net.preferIPv4Stack" , "true");
		
		config.setPort(portNumber);
		config.setHostname("0.0.0.0");
		if(useSSL) {
			try {
				InputStream stream = new FileInputStream(keystore);
				config.setKeyStore(stream);
				config.setKeyStorePassword(storepass);
			} catch(FileNotFoundException e) {
				logger.error("Keystore {} not found on filesystem", keystore);
			}
		}
		server = new SocketIOServer(config);

		server.addEventListener("setFeedback", Feedback.class,
				new DataListener<Feedback>() {
					@Override
					public void onData(SocketIOClient client, Feedback data) {
						/**
						 * do a check if user is in the session, for which he would give a feedback
						 */
						User u = socketid2user.get(client.getSessionId());
						if(u == null || sessionService.isUserInSession(u, data.getSessionkey()) == false) {
							return;
						}
						sessionService.postFeedback(data.getSessionkey(), data.getValue(), u);
						
						/**
						 * collect a list of users which are in the current session
						 * iterate over all connected clients and if send feedback, 
						 * if user is in current session
						 */
						List<String> users = sessionService.getUsersInSession(data.getSessionkey());
						de.thm.arsnova.entities.Feedback fb = sessionService.getFeedback(data.getSessionkey());
						
						for(SocketIOClient c : server.getAllClients()) {
							u = socketid2user.get(c.getSessionId());
							if(u != null && users.contains(u.getUsername())) {
								c.sendEvent("updateFeedback", fb.getValues());
							}
						}
					}
		});

		
		
		
		server.addConnectListener(new ConnectListener() {
	        @Override
	        public void onConnect(SocketIOClient client) {
	        	logger.info("addConnectListener.onConnect: Client: {}", new Object[] {client});
	        }
	    });
		
		server.addDisconnectListener(new DisconnectListener() {
	        @Override
	        public void onDisconnect(SocketIOClient client) {
	        	logger.info("addDisconnectListener.onDisconnect: Client: {}", new Object[] {client});
	        	socketid2user.remove(client.getSessionId());
	        }
	    });
		
		server.start();
	}

	public void stopServer() throws Exception {
		logger.debug("In stopServer method of class: {}", getClass().getName());
		for(SocketIOClient client : server.getAllClients()) {
			client.disconnect();
		}
		server.stop();		

	}

	public int getPortNumber() {
		return portNumber;
	}

	@Required
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public String getStorepass() {
		return storepass;
	}

	@Required
	public void setStorepass(String storepass) {
		this.storepass = storepass;
	}

	public String getKeystore() {
		return keystore;
	}

	@Required
	public void setKeystore(String keystore) {
		this.keystore = keystore;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	@Required
	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public boolean authorize(UUID session, User user) {
		return socketid2user.put(session, user) != null;		
	}
	
	public void reportDeletedFeedback(String username, Set<String> arsSessions) {
		UUID connectionId = findConnectionIdForUser(username);
		if (connectionId == null) {
			return;
		}
		
		for (SocketIOClient client : server.getAllClients()) {
			// Find the client whose feedback has been deleted and send a message.
			if (client.getSessionId().compareTo(connectionId) == 0) {
				client.sendEvent("removedFeedback", arsSessions);
				break;
			}
		}
	}

	private UUID findConnectionIdForUser(String username) {
		for (Map.Entry<UUID, User> e : socketid2user.entrySet()) {
			User u = e.getValue();
			if (u.getUsername().equals(username)) {
				return e.getKey();
			}
		}
		return null;
	}
	
	
	public void reportUpdatedFeedbackForSessions(Set<String> allAffectedSessions) {
		for (String sessionKey : allAffectedSessions) {
			de.thm.arsnova.entities.Feedback fb = sessionService.getFeedback(sessionKey);
			server.getBroadcastOperations().sendEvent("updateFeedback", fb.getValues());
		}
	}
}