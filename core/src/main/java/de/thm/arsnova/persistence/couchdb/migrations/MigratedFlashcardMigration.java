package de.thm.arsnova.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.properties.CouchDbMigrationProperties;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration adjusts Contents which have been created through migration of
 * a legacy flashcard before. These flashcards have been previously been stored
 * as slide or text content.
 *
 * @author Daniel Gerhardt
 */
@Service
@ConditionalOnProperty(
    name = "enabled",
    prefix = CouchDbMigrationProperties.PREFIX)
public class MigratedFlashcardMigration extends AbstractMigration {
  private static final String ID = "20210223155000";
  private static final String CONTENT_FLASHCARD_INDEX = "content-flashcard-index";

  public MigratedFlashcardMigration(
      final MangoCouchDbConnector connector) {
    super(ID, connector);
  }

  @PostConstruct
  public void initMigration() {
    addEntityMigrationStepHandler(
        ContentMigrationEntity.class,
        CONTENT_FLASHCARD_INDEX,
        Map.of(
            "type", "Content",
            "extensions.v2.format", "flashcard"
        ),
        content -> {
          content.setFormat(Content.Format.FLASHCARD);
          content.getExtensions().remove("v2");
          return List.of(content);
        }
    );
  }

  private static class ContentMigrationEntity extends MigrationEntity {
    private Content.Format format;
    private String additionalTextTitle;
    private Map<String, Object> extensions;

    @JsonView(View.Persistence.class)
    public Content.Format getFormat() {
      return format;
    }

    @JsonView(View.Persistence.class)
    public void setFormat(final Content.Format format) {
      this.format = format;
    }

    /* Value obsolete for flashcards. Do not serialize. */
    @JsonIgnore
    public String getAdditionalTextTitle() {
      return additionalTextTitle;
    }

    @JsonView(View.Persistence.class)
    public void setAdditionalTextTitle(final String additionalTextTitle) {
      this.additionalTextTitle = additionalTextTitle;
    }

    @JsonView(View.Persistence.class)
    public Map<String, Object> getExtensions() {
      return extensions;
    }

    @JsonView(View.Persistence.class)
    public void setExtensions(final Map<String, Object> extensions) {
      this.extensions = extensions;
    }
  }
}
