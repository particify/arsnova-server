package net.particify.arsnova.core.management;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.event.AfterCreationEvent;
import net.particify.arsnova.core.event.AfterDeletionEvent;
import net.particify.arsnova.core.event.NewFeedbackEvent;
import net.particify.arsnova.core.event.ReadRepositoryEvent;
import net.particify.arsnova.core.event.WriteRepositoryEvent;
import net.particify.arsnova.core.model.Answer;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.UserProfile;

@Service
public class MetricsService {
  private static final String EVENT_PREFIX = "arsnova.events.";
  private static final String CRUD_EVENT_NAME = EVENT_PREFIX + "crud";
  private static final String USER_EVENT_NAME = EVENT_PREFIX + "user";
  private static final String SURVEY_VOTE_EVENT_NAME = EVENT_PREFIX + "survey.vote";
  private static final String EVENT_TYPE_KEY = "event.type";
  private static final String ENTITY_TYPE_KEY = "entity.type";
  private static final String AUTH_PROVIDER_TYPE_KEY = "auth.provider.type";
  private static final String CREATION_EVENT_TYPE = "creation";
  private static final String DELETION_EVENT_TYPE = "deletion";
  private static final String ACCOUNT_AUTH_PROVIDER_TYPE = "account";
  private static final String GUEST_AUTH_PROVIDER_TYPE = "guest";
  private static final String SSO_AUTH_PROVIDER_TYPE = "sso";
  private static final String REPOSITORY_READ_EVENT_NAME = EVENT_PREFIX + "repository.read";
  private static final String REPOSITORY_WRITE_EVENT_NAME = EVENT_PREFIX + "repository.write";
  private static final String REPOSITORY_TYPE_KEY = "type";
  private static final String REPOSITORY_OPERATION_MODE_KEY = "operation.mode";
  private static final String REPOSITORY_ENTITY_MODE_KEY = "entity.mode";

  private final MeterRegistry meterRegistry;

  private final Counter accountUsersCreatedCounter;
  private final Counter guestUsersCreatedCounter;
  private final Counter ssoUsersCreatedCounter;
  private final Counter accountUsersDeletedCounter;
  private final Counter guestUsersDeletedCounter;
  private final Counter ssoUsersDeletedCounter;

  private final Counter userProfileCreatedCounter;
  private final Counter userProfileDeletedCounter;
  private final Counter roomCreatedCounter;
  private final Counter roomDeletedCounter;
  private final Counter contentCreatedCounter;
  private final Counter contentDeletedCounter;
  private final Counter answerCreatedCounter;
  private final Counter answerDeletedCounter;

  private final Counter surveyVoteCounter;

  public MetricsService(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;

    accountUsersCreatedCounter = meterRegistry.counter(USER_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, CREATION_EVENT_TYPE),
        Tag.of(AUTH_PROVIDER_TYPE_KEY, ACCOUNT_AUTH_PROVIDER_TYPE)));
    guestUsersCreatedCounter = meterRegistry.counter(USER_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, CREATION_EVENT_TYPE),
        Tag.of(AUTH_PROVIDER_TYPE_KEY, GUEST_AUTH_PROVIDER_TYPE)));
    ssoUsersCreatedCounter = meterRegistry.counter(USER_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, CREATION_EVENT_TYPE),
        Tag.of(AUTH_PROVIDER_TYPE_KEY, SSO_AUTH_PROVIDER_TYPE)));

    accountUsersDeletedCounter = meterRegistry.counter(USER_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, DELETION_EVENT_TYPE),
        Tag.of(AUTH_PROVIDER_TYPE_KEY, ACCOUNT_AUTH_PROVIDER_TYPE)));
    guestUsersDeletedCounter = meterRegistry.counter(USER_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, DELETION_EVENT_TYPE),
        Tag.of(AUTH_PROVIDER_TYPE_KEY, GUEST_AUTH_PROVIDER_TYPE)));
    ssoUsersDeletedCounter = meterRegistry.counter(USER_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, DELETION_EVENT_TYPE),
        Tag.of(AUTH_PROVIDER_TYPE_KEY, SSO_AUTH_PROVIDER_TYPE)));

    userProfileCreatedCounter = meterRegistry.counter(CRUD_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, CREATION_EVENT_TYPE),
        Tag.of(ENTITY_TYPE_KEY, UserProfile.class.getSimpleName())));
    userProfileDeletedCounter = meterRegistry.counter(CRUD_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, DELETION_EVENT_TYPE),
        Tag.of(ENTITY_TYPE_KEY, UserProfile.class.getSimpleName())));
    roomCreatedCounter = meterRegistry.counter(CRUD_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, CREATION_EVENT_TYPE),
        Tag.of(ENTITY_TYPE_KEY, Room.class.getSimpleName())));
    roomDeletedCounter = meterRegistry.counter(CRUD_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, DELETION_EVENT_TYPE),
        Tag.of(ENTITY_TYPE_KEY, Room.class.getSimpleName())));
    contentCreatedCounter = meterRegistry.counter(CRUD_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, CREATION_EVENT_TYPE),
        Tag.of(ENTITY_TYPE_KEY, Content.class.getSimpleName())));
    contentDeletedCounter = meterRegistry.counter(CRUD_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, DELETION_EVENT_TYPE),
        Tag.of(ENTITY_TYPE_KEY, Content.class.getSimpleName())));
    answerCreatedCounter = meterRegistry.counter(CRUD_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, CREATION_EVENT_TYPE),
        Tag.of(ENTITY_TYPE_KEY, Answer.class.getSimpleName())));
    answerDeletedCounter = meterRegistry.counter(CRUD_EVENT_NAME, List.of(
        Tag.of(EVENT_TYPE_KEY, DELETION_EVENT_TYPE),
        Tag.of(ENTITY_TYPE_KEY, Answer.class.getSimpleName())));

    surveyVoteCounter = meterRegistry.counter(SURVEY_VOTE_EVENT_NAME);
  }

  @EventListener
  public void countCreatedUser(final AfterCreationEvent<UserProfile> event) {
    userProfileCreatedCounter.increment();
    if (event.getEntity().getAuthProvider() == UserProfile.AuthProvider.ARSNOVA) {
      accountUsersCreatedCounter.increment();
    } else if (event.getEntity().getAuthProvider() == UserProfile.AuthProvider.ARSNOVA_GUEST) {
      guestUsersCreatedCounter.increment();
    } else {
      ssoUsersCreatedCounter.increment();
    }
  }

  @EventListener
  public void countDeletedUser(final AfterDeletionEvent<UserProfile> event) {
    userProfileDeletedCounter.increment();
    if (event.getEntity().getAuthProvider() == UserProfile.AuthProvider.ARSNOVA) {
      accountUsersDeletedCounter.increment();
    } else if (event.getEntity().getAuthProvider() == UserProfile.AuthProvider.ARSNOVA_GUEST) {
      guestUsersDeletedCounter.increment();
    } else {
      ssoUsersDeletedCounter.increment();
    }
  }

  @EventListener
  public void countCreatedRoom(final AfterCreationEvent<Room> event) {
    roomCreatedCounter.increment();
  }

  @EventListener
  public void countDeletedRoom(final AfterDeletionEvent<Room> event) {
    roomDeletedCounter.increment();
  }

  @EventListener
  public void countCreatedContent(final AfterCreationEvent<? extends Content> event) {
    contentCreatedCounter.increment();
  }

  @EventListener
  public void countDeletedContent(final AfterDeletionEvent<? extends Content> event) {
    contentDeletedCounter.increment();
  }

  @EventListener
  public void countCreatedAnswer(final AfterCreationEvent<? extends Answer> event) {
    answerCreatedCounter.increment();
  }

  @EventListener
  public void countDeletedAnswer(final AfterDeletionEvent<? extends Answer> event) {
    answerDeletedCounter.increment();
  }

  @EventListener
  public void countSurveyVote(final NewFeedbackEvent event) {
    surveyVoteCounter.increment();
  }

  @EventListener
  public void handleReadRepositoryEvent(final ReadRepositoryEvent event) {
    final Counter counter = meterRegistry.counter(REPOSITORY_READ_EVENT_NAME, List.of(
        Tag.of(REPOSITORY_TYPE_KEY, event.getTypeName()),
        Tag.of(REPOSITORY_OPERATION_MODE_KEY, event.isReduced()
            ? "aggregated"
            : (event.isMultiple() ? "list" : "single")),
        Tag.of(REPOSITORY_ENTITY_MODE_KEY, event.isReduced()
            ? "n/a"
            : event.isPartial() ? "partial" : "full")
    ));
    counter.increment();
  }

  @EventListener
  public void handleWriteRepositoryEvent(final WriteRepositoryEvent event) {
    final Counter counter = meterRegistry.counter(REPOSITORY_WRITE_EVENT_NAME, List.of(
        Tag.of(REPOSITORY_TYPE_KEY, event.getTypeName()),
        Tag.of(REPOSITORY_OPERATION_MODE_KEY, event.isMultiple() ? "bulk" : "single")
    ));
    counter.increment();
  }
}
