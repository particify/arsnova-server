package net.particify.arsnova.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import net.particify.arsnova.core.persistence.AccessTokenRepository;
import net.particify.arsnova.core.persistence.AnnouncementRepository;
import net.particify.arsnova.core.persistence.AnswerRepository;
import net.particify.arsnova.core.persistence.ContentGroupRepository;
import net.particify.arsnova.core.persistence.ContentGroupTemplateRepository;
import net.particify.arsnova.core.persistence.ContentRepository;
import net.particify.arsnova.core.persistence.ContentTemplateRepository;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.persistence.RoomRepository;
import net.particify.arsnova.core.persistence.RoomSettingsRepository;
import net.particify.arsnova.core.persistence.RoomUserAliasRepository;
import net.particify.arsnova.core.persistence.StatisticsRepository;
import net.particify.arsnova.core.persistence.TemplateTagRepository;
import net.particify.arsnova.core.persistence.UserRepository;
import net.particify.arsnova.core.persistence.ViolationReportRepository;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {
  AccessTokenRepository.class,
  AnnouncementRepository.class,
  AnswerRepository.class,
  ContentGroupRepository.class,
  ContentGroupTemplateRepository.class,
  ContentRepository.class,
  ContentTemplateRepository.class,
  DeletionRepository.class,
  MangoCouchDbConnector.class,
  RoomRepository.class,
  RoomSettingsRepository.class,
  RoomUserAliasRepository.class,
  StatisticsRepository.class,
  TemplateTagRepository.class,
  UserRepository.class,
  ViolationReportRepository.class
})
public @interface SharedRepositoryMocks {
}
