package de.thm.arsnova.config;

import de.thm.arsnova.persistance.*;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class TestPersistanceConfig {
	@Bean
	public LogEntryRepository logEntryRepository() {
		return Mockito.mock(LogEntryRepository.class);
	}

	@Bean
	public UserRepository userRepository() {
		return Mockito.mock(UserRepository.class);
	}

	@Bean
	public RoomRepository sessionRepository() {
		return Mockito.mock(RoomRepository.class);
	}

	@Bean
	public CommentRepository commentRepository() {
		return Mockito.mock(CommentRepository.class);
	}

	@Bean
	public ContentRepository contentRepository() {
		return Mockito.mock(ContentRepository.class);
	}

	@Bean
	public AnswerRepository answerRepository() {
		return Mockito.mock(AnswerRepository.class);
	}

	@Bean
	public AttachmentRepository attachmentRepository() {
		return Mockito.mock(AttachmentRepository.class);
	}

	@Bean
	public MotdRepository motdRepository() {
		return Mockito.mock(MotdRepository.class);
	}

	@Bean
	public StatisticsRepository statisticsRepository() {
		return Mockito.mock(StatisticsRepository.class);
	}

	@Bean
	public SessionStatisticsRepository sessionStatisticsRepository() {
		return Mockito.mock(SessionStatisticsRepository.class);
	}
}
