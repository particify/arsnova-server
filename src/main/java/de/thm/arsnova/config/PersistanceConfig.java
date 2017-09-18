package de.thm.arsnova.config;

import de.thm.arsnova.entities.serialization.CouchDbObjectMapperFactory;
import de.thm.arsnova.persistance.*;
import de.thm.arsnova.persistance.couchdb.*;
import org.ektorp.CouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.spring.HttpClientFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class PersistanceConfig {
	@Value("${couchdb.name}") private String couchDbName;
	@Value("${couchdb.host}") private String couchDbHost;
	@Value("${couchdb.port}") private int couchDbPort;
	@Value("${couchdb.username:}") private String couchDbUsername;
	@Value("${couchdb.password:}") private String couchDbPassword;

	@Bean
	public CouchDbConnector couchDbConnector() throws Exception {
		return new InitializingCouchDbConnector(couchDbName, couchDbInstance(), new CouchDbObjectMapperFactory());
	}

	@Bean
	public StdCouchDbInstance couchDbInstance() throws Exception {
		return new StdCouchDbInstance(couchDbHttpClientFactory().getObject());
	}

	@Bean
	public HttpClientFactoryBean couchDbHttpClientFactory() throws Exception {
		final HttpClientFactoryBean factory = new HttpClientFactoryBean();
		factory.setHost(couchDbHost);
		factory.setPort(couchDbPort);
		if (!couchDbUsername.isEmpty()) {
			factory.setUsername(couchDbUsername);
			factory.setPassword(couchDbPassword);
		}

		return factory;
	}

	@Bean
	public LogEntryRepository logEntryRepository() throws Exception {
		return new CouchDbLogEntryRepository(couchDbConnector(), false);
	}

	@Bean
	public UserRepository userRepository() throws Exception {
		return new CouchDbUserRepository(couchDbConnector(), false);
	}

	@Bean
	public RoomRepository sessionRepository() throws Exception {
		return new CouchDbRoomRepository(couchDbConnector(), false);
	}

	@Bean
	public CommentRepository commentRepository() throws Exception {
		return new CouchDbCommentRepository(couchDbConnector(), false);
	}

	@Bean
	public ContentRepository contentRepository() throws Exception {
		return new CouchDbContentRepository(couchDbConnector(), false);
	}

	@Bean
	public AnswerRepository answerRepository() throws Exception {
		return new CouchDbAnswerRepository(couchDbConnector(), false);
	}

	@Bean
	public MotdRepository motdRepository() throws Exception {
		return new CouchDbMotdRepository(couchDbConnector(), false);
	}

	@Bean
	public MotdListRepository motdListRepository() throws Exception {
		return new CouchDbMotdListRepository(couchDbConnector(), false);
	}

	@Bean
	public VisitedSessionRepository visitedSessionRepository() throws Exception {
		return new CouchDbVisitedSessionRepository(couchDbConnector(), false);
	}

	@Bean
	public StatisticsRepository statisticsRepository() throws Exception {
		return new CouchDbStatisticsRepository(couchDbConnector(), false);
	}

	@Bean
	public SessionStatisticsRepository sessionStatisticsRepository() throws Exception {
		return new CouchDbSessionStatisticsRepository(couchDbConnector(), false);
	}
}
