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

package de.thm.arsnova.websocket.message;

import java.util.List;

import de.thm.arsnova.model.AnswerStatistics;

public class AnswersChanged extends WebSocketMessage<AnswersChanged.AnswersChangedPayload> {

  public static class AnswersChangedPayload implements WebSocketPayload {
    private List<String> ids;
    private AnswerStatistics stats;

    public List<String> getIds() {
      return ids;
    }

    public void setIds(final List<String> ids) {
      this.ids = ids;
    }

    public AnswerStatistics getStats() {
      return stats;
    }

    public void setStats(final AnswerStatistics stats) {
      this.stats = stats;
    }
  }

  public AnswersChanged(final List<String> ids, final AnswerStatistics stats) {
    super(AnswersChanged.class.getSimpleName());
    final AnswersChangedPayload payload = new AnswersChangedPayload();
    payload.ids = ids;
    payload.stats = stats;
    this.setPayload(payload);
  }
}
