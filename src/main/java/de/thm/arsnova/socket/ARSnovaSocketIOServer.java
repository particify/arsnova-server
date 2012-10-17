package de.thm.arsnova.socket;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.socket.message.Feedback;
import de.thm.arsnova.socket.message.Question;

public class ARSnovaSocketIOServer {

	@Autowired
	private ISessionService sessionService;	
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final ConcurrentHashMap<UUID, User> socketid2user = new ConcurrentHashMap<UUID, User>();
	
	private int portNumber;
	private String hostIp;
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
		config.setHostname(hostIp);
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
					public void onData(SocketIOClient client, Feedback data, AckRequest ackSender) {
						/**
						 * do a check if user is in the session, for which he would give a feedback
						 */
						User u = socketid2user.get(client.getSessionId());
						if(u == null || sessionService.isUserInSession(u, data.getSessionkey()) == false) {
							return;
						}
						sessionService.saveFeedback(data.getSessionkey(), data.getValue(), u);
						
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
		
		server.addEventListener("arsnova/question/create", Question.class, new DataListener<Question>(){
			@Override
			public void onData(SocketIOClient client, Question question, AckRequest ackSender) {
				sessionService.saveQuestion(question);
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

	public String getHostIp() {
		return hostIp;
	}

	public void setHostIp(String hostIp) {
		this.hostIp = hostIp;
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

	public void authorize(UUID session, User user) {
		socketid2user.put(session, user);
	}
	
	public void reportDeletedFeedback(String username, Set<String> arsSessions) {
		List<UUID> connectionIds = findConnectionIdForUser(username);
		if (connectionIds.isEmpty()) {
			return;
		}
		
		for (SocketIOClient client : server.getAllClients()) {
			// Find the client whose feedback has been deleted and send a message.
			if(connectionIds.contains(client.getSessionId())) {
				client.sendEvent("removedFeedback", arsSessions);
			}
		}
	}

	private List<UUID> findConnectionIdForUser(String username) {
		List<UUID> result = new ArrayList<UUID>();
		for (Entry<UUID, User> e : socketid2user.entrySet()) {
			if (e.getValue().getUsername().equals(username)) {
				result.add(e.getKey());
			}
		}
		return result;
	}
	
	
	public void reportUpdatedFeedbackForSessions(Set<String> allAffectedSessions) {
		for (String sessionKey : allAffectedSessions) {
			de.thm.arsnova.entities.Feedback fb = sessionService.getFeedback(sessionKey);
			server.getBroadcastOperations().sendEvent("updateFeedback", fb.getValues());
		}
	}
}