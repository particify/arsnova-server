package de.thm.arsnova.config;

import de.thm.arsnova.model.serialization.CouchDbObjectMapperFactory;
import de.thm.arsnova.persistence.*;
import de.thm.arsnova.persistence.couchdb.*;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.spring.HttpClientFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@ComponentScan({
		"de.thm.arsnova.persistence.couchdb"
})
@Configuration
@Profile("!test")
public class PersistenceConfig {
	private static final int MIGRATION_SOCKET_TIMEOUT = 30000;

	@Value("${couchdb.name}") private String couchDbName;
	@Value("${couchdb.host}") private String couchDbHost;
	@Value("${couchdb.port}") private int couchDbPort;
	@Value("${couchdb.username:}") private String couchDbUsername;
	@Value("${couchdb.password:}") private String couchDbPassword;
	@Value("${couchdb.migrate-from:}") private String couchDbMigrateFrom;

	@Bean
	@Primary
	public MangoCouchDbConnector couchDbConnector() throws Exception {
		return new MangoCouchDbConnector(couchDbName, couchDbInstance(), couchDbObjectMapperFactory());
	}

	@Bean
	public MangoCouchDbConnector couchDbMigrationConnector() throws Exception {
		if (couchDbMigrateFrom.isEmpty()) {
			return null;
		}
		return new MangoCouchDbConnector(couchDbMigrateFrom, couchDbInstance(), couchDbObjectMapperFactory());
	}

	@Bean
	public StdCouchDbInstance couchDbInstance() throws Exception {
		return new StdCouchDbInstance(couchDbHttpClientFactory().getObject());
	}

	@Bean
	public StdCouchDbInstance couchDbMigrationInstance() throws Exception {
		return new StdCouchDbInstance(couchDbMigrationHttpClientFactory().getObject());
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
	public HttpClientFactoryBean couchDbMigrationHttpClientFactory() throws Exception {
		final HttpClientFactoryBean factory = couchDbHttpClientFactory();
		factory.setSocketTimeout(MIGRATION_SOCKET_TIMEOUT);

		return factory;
	}

	@Bean
	public CouchDbObjectMapperFactory couchDbObjectMapperFactory() {
		return new CouchDbObjectMapperFactory();
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
	public StatisticsRepository statisticsRepository() throws Exception {
		return new CouchDbStatisticsRepository(couchDbConnector(), false);
	}

	@Bean
	public SessionStatisticsRepository sessionStatisticsRepository() throws Exception {
		return new CouchDbSessionStatisticsRepository(couchDbConnector(), false);
	}
}
