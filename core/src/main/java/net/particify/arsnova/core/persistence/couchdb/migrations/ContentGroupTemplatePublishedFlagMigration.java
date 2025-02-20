package net.particify.arsnova.core.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration sets the published flag to true for
 * {@link net.particify.arsnova.core.model.ContentGroupTemplate}s that do not
 * have this flag.
 */
@Service
public class ContentGroupTemplatePublishedFlagMigration extends AbstractMigration {
  private static final String ID = "20250220114300";
  private static final String TEMPLATE_INDEX = "template-published-index";

  public ContentGroupTemplatePublishedFlagMigration(
      final MangoCouchDbConnector connector) {
    super(ID, connector);
  }

  @PostConstruct
  public void initMigration() {
    addEntityMigrationStepHandler(
        ContentGroupTemplateMigrationEntity.class,
        TEMPLATE_INDEX,
        Map.of(
            "type", "ContentGroupTemplate",
            "published", Map.of("$exists", false)
        ),
        template -> {
          template.setPublished(true);
          return List.of(template);
        }
    );
  }

  private static class ContentGroupTemplateMigrationEntity extends MigrationEntity {
    private boolean published;

    @JsonView(View.Persistence.class)
    public boolean isPublished() {
      return published;
    }

    @JsonView(View.Persistence.class)
    public void setPublished(final boolean published) {
      this.published = published;
    }
  }
}
