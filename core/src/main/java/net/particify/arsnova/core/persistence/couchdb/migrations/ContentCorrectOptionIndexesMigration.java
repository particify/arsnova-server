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

package net.particify.arsnova.core.persistence.couchdb.migrations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.core.style.ToStringCreator;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

/**
 * This migration fixes correctOptionIndexes of Contents which have been set
 * incorrectly by the webclient. During editing older version of the client only
 * set the points which did work correctly while the client used those points
 * to determine correct options.
 *
 * <p>
 * Furthermore, those points are removed from the
 * {@link net.particify.arsnova.core.model.ChoiceQuestionContent.AnswerOption}s.
 * </p>
 *
 * @author Daniel Gerhardt
 */
@Service
public class ContentCorrectOptionIndexesMigration extends AbstractMigration {
  private static final String ID = "20210325161700";
  private static final String CONTENT_INDEX = "content-index";
  private static final Map<String, Boolean> existsSelector = Map.of("$exists", true);

  public ContentCorrectOptionIndexesMigration(
      final MangoCouchDbConnector connector) {
    super(ID, connector);
  }

  @PostConstruct
  public void initMigration() {
    addEntityMigrationStepHandler(
        ContentMigrationEntity.class,
        CONTENT_INDEX,
        Map.of(
            "type", "Content",
            /* Filter by options array which needs to contain at least one element with a points property */
            "options", Map.of("$elemMatch", Map.of("points", existsSelector))
        ),
        content -> {
          if (content.getCorrectOptionIndexes().isEmpty()) {
            content.setCorrectOptionIndexes(
                IntStream.range(0, content.getOptions().size())
                    .filter(i -> content.getOptions().get(i).getPoints() > 0)
                    .boxed()
                    .collect(Collectors.toList()));
          }
          // Return the content for updating in any case so points are
          // removed from the options.
          return List.of(content);
        }
    );
  }

  /**
   * This class is used to access legacy properties for Contents which are no
   * longer part of the domain model.
   */
  private static class ContentMigrationEntity extends MigrationEntity {
    private List<AnswerOptionMigrationEntity> options;
    private List<Integer> correctOptionIndexes;

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

    @Override
    public String toString() {
      return new ToStringCreator(this)
          .append("options", options)
          .append("correctOptionIndexes", correctOptionIndexes)
          .append("[properties]", getProperties())
          .toString();
    }

    private static class AnswerOptionMigrationEntity extends InnerMigrationEntity {
      private int points;

      /* Legacy property: Do not serialize. */
      @JsonIgnore
      public int getPoints() {
        return points;
      }

      @JsonView(View.Persistence.class)
      public void setPoints(final int points) {
        this.points = points;
      }
    }
  }
}
