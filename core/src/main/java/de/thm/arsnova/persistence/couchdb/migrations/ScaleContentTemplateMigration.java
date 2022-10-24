/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration replaces custom options of SCALE format contents with
 * predefined option templates where possible. If the template is not an
 * appropriate replacement, the format is chanced to CHOICE instead.
 *
 * @author Daniel Gerhardt
 */
@Service
public class ScaleContentTemplateMigration extends AbstractMigration {
  private static final String ID = "20210704105600";
  private static final String CONTENT_INDEX = "scale-content-index";
  private static final Map<String, Boolean> notExistsSelector = Map.of("$exists", false);
  private static final int DEFAULT_OPTION_COUNT = 5;
  private static final String AGREEMENT_OPTION_PATTERN =
      "strongly.* agree|(stimme|trifft)( (voll(kommen| und ganz)?|völlig|komplett))? zu";
  private static final Logger logger = LoggerFactory.getLogger(ScaleContentTemplateMigration.class);

  public ScaleContentTemplateMigration(
      final MangoCouchDbConnector connector) {
    super(ID, connector);
  }

  @PostConstruct
  public void initMigration() {
    final Pattern optionPattern = Pattern.compile(AGREEMENT_OPTION_PATTERN, Pattern.CASE_INSENSITIVE);
    addEntityMigrationStepHandler(
        ContentMigrationEntity.class,
        CONTENT_INDEX,
        Map.of(
            "type", "Content",
            "format", "SCALE",
            "optionTemplate", notExistsSelector
        ),
        content -> {
          if (content.getOptions().size() == DEFAULT_OPTION_COUNT
              && optionPattern.matcher(content.getOptions().get(0).label).find()) {
            content.setOptionTemplate(ContentMigrationEntity.ScaleOptionTemplate.AGREEMENT);
            content.setOptionCount(DEFAULT_OPTION_COUNT);
            content.setOptions(null);
            content.setCorrectOptionIndexes(null);
          } else {
            logger.debug("Could not apply template for options: {}", content);
            content.setFormat(ContentMigrationEntity.Format.CHOICE);
          }
          return List.of(content);
        }
    );
  }

  /**
   * This class is used to access legacy properties for Contents which are no
   * longer part of the domain model.
   */
  private static class ContentMigrationEntity extends MigrationEntity {
    enum Format {
      CHOICE,
      SCALE
    }

    enum ScaleOptionTemplate {
      AGREEMENT
    }

    private Format format;
    private List<AnswerOptionMigrationEntity> options;
    private List<Integer> correctOptionIndexes;
    private ScaleOptionTemplate optionTemplate;
    private int optionCount;

    @JsonView(View.Persistence.class)
    public Format getFormat() {
      return format;
    }

    @JsonView(View.Persistence.class)
    public void setFormat(final Format format) {
      this.format = format;
    }

    @JsonView(View.Persistence.class)
    public List<AnswerOptionMigrationEntity> getOptions() {
      return options;
    }

    @JsonView(View.Persistence.class)
    public void setOptions(final List<AnswerOptionMigrationEntity> options) {
      this.options = options;
    }

    @JsonView(View.Persistence.class)
    public List<Integer> getCorrectOptionIndexes() {
      return correctOptionIndexes;
    }

    @JsonView(View.Persistence.class)
    public void setCorrectOptionIndexes(final List<Integer> correctOptionIndexes) {
      this.correctOptionIndexes = correctOptionIndexes;
    }

    @JsonView(View.Persistence.class)
    public ScaleOptionTemplate getOptionTemplate() {
      return optionTemplate;
    }

    @JsonView(View.Persistence.class)
    public void setOptionTemplate(final ScaleOptionTemplate optionTemplate) {
      this.optionTemplate = optionTemplate;
    }

    @JsonView(View.Persistence.class)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public int getOptionCount() {
      return optionCount;
    }

    @JsonView(View.Persistence.class)
    public void setOptionCount(final int optionCount) {
      this.optionCount = optionCount;
    }

    @Override
    public String toString() {
      return new ToStringCreator(this)
          .append("format", format)
          .append("options", options)
          .append("correctOptionIndexes", correctOptionIndexes)
          .append("optionTemplate", optionTemplate)
          .append("optionCount", optionCount)
          .append("[properties]", getProperties())
          .toString();
    }

    private static class AnswerOptionMigrationEntity extends InnerMigrationEntity {
      private String label;

      @JsonView(View.Persistence.class)
      public String getLabel() {
        return label;
      }

      @JsonView(View.Persistence.class)
      public void setLabel(final String label) {
        this.label = label;
      }

      @Override
      public String toString() {
        return new ToStringCreator(this)
            .append("label", label)
            .toString();
      }
    }
  }
}
