package net.particify.arsnova.core.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration copies the last access data from the authz service to the user profile. If there is no last access,
 * the current time is used unless the lastLoginTimestamp legacy property is set.
 *
 * @author Daniel Gerhardt
 */
@Service
public class LastActivityMigration extends AbstractMigration {
  private static final String ID = "202409191322";
  private static final String USERPROFILE_INDEX = "userprofile-index";
  private RestClient authzRestClient;

  public LastActivityMigration(
      final MangoCouchDbConnector connector,
      final SystemProperties systemProperties) {
    super(ID, connector);
    final String authzUrl = systemProperties.getAuthzServiceUrl();
    this.authzRestClient = RestClient.create(authzUrl);
  }

  @PostConstruct
  public void initMigration() {
    addEntityMigrationStepHandler(
        UserProfileMigrationEntity.class,
        USERPROFILE_INDEX,
        Map.of(
          "type", "UserProfile",
          "lastActivityTimestamp", Map.of("$exists", false)
        ),
        userProfile -> {
          final var lastAccess = fetchLastAccess(userProfile.getId());
          final var lastActivityTimestamp = lastAccess != null
              ? lastAccess.lastAccess
              : (userProfile.getLastLoginTimestamp() != null ? userProfile.getLastLoginTimestamp() : Instant.now());
          userProfile.setLastActivityTimestamp(lastActivityTimestamp.compareTo(
              userProfile.getCreationTimestamp().toInstant()) > 0
              ? lastActivityTimestamp
              : userProfile.getCreationTimestamp().toInstant());
          return List.of(userProfile);
        }
    );
  }

  private LastAccess fetchLastAccess(final String userId) {
    return authzRestClient
      .get()
      .uri(uriBuilder -> uriBuilder
        .path("/roomaccess/last-access")
        .pathSegment(userId)
        .build())
      .retrieve()
      .body(new ParameterizedTypeReference<>() {});
  }

  private static class UserProfileMigrationEntity extends MigrationEntity {
    private Instant lastLoginTimestamp;
    private Instant lastActivityTimestamp;

    @JsonView(View.Persistence.class)
    public Instant getLastActivityTimestamp() {
      return lastActivityTimestamp;
    }

    @JsonView(View.Persistence.class)
    public void setLastActivityTimestamp(final Instant lastActivityTimestamp) {
      this.lastActivityTimestamp = lastActivityTimestamp;
    }

    /* Legacy property: Do not serialize. */
    @JsonIgnore
    public Instant getLastLoginTimestamp() {
      return lastLoginTimestamp;
    }

    @JsonView(View.Persistence.class)
    public void setLastLoginTimestamp(final Instant lastLoginTimestamp) {
      this.lastLoginTimestamp = lastLoginTimestamp;
    }
  }

  private record LastAccess(String userId, Instant lastAccess) {}
}
