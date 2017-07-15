package de.thm.arsnova.config;

import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.entities.LogEntry;
import de.thm.arsnova.entities.Motd;
import de.thm.arsnova.entities.MotdList;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.VisitedSession;
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

		return factory;
	}

	@Bean
	public LogEntryRepository logEntryRepository() throws Exception {
		return new CouchDbLogEntryRepository(LogEntry.class, couchDbConnector(), false);
	}

	@Bean
	public UserRepository userRepository() throws Exception {
		return new CouchDbUserRepository(DbUser.class, couchDbConnector(), false);
	}

	@Bean
	public SessionRepository sessionRepository() throws Exception {
		return new CouchDbSessionRepository(Session.class, couchDbConnector(), false);
	}

	@Bean
	public CommentRepository commentRepository() throws Exception {
		return new CouchDbCommentRepository(Comment.class, couchDbConnector(), false);
	}

	@Bean
	public ContentRepository contentRepository() throws Exception {
		return new CouchDbContentRepository(Content.class, couchDbConnector(), false);
	}

	@Bean
	public AnswerRepository answerRepository() throws Exception {
		return new CouchDbAnswerRepository(Answer.class, couchDbConnector(), false);
	}

	@Bean
	public MotdRepository motdRepository() throws Exception {
		return new CouchDbMotdRepository(Motd.class, couchDbConnector(), false);
	}

	@Bean
	public MotdListRepository motdListRepository() throws Exception {
		return new CouchDbMotdListRepository(MotdList.class, couchDbConnector(), false);
	}

	@Bean
	public VisitedSessionRepository visitedSessionRepository() throws Exception {
		return new CouchDbVisitedSessionRepository(VisitedSession.class, couchDbConnector(), false);
	}

	@Bean
	public StatisticsRepository statisticsRepository() throws Exception {
		return new CouchDbStatisticsRepository(Object.class, couchDbConnector(), false);
	}

	@Bean
	public SessionStatisticsRepository sessionStatisticsRepository() throws Exception {
		return new CouchDbSessionStatisticsRepository(Object.class, couchDbConnector(), false);
	}
}
