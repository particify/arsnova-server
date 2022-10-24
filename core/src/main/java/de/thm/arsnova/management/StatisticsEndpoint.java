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

package de.thm.arsnova.management;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import de.thm.arsnova.model.Statistics;
import de.thm.arsnova.service.StatisticsService;

@Component
@Endpoint(id = "stats")
public class StatisticsEndpoint {
  private StatisticsService statisticsService;

  public StatisticsEndpoint(final StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  @ReadOperation
  public Statistics readStatistics() {
    return statisticsService.getStatistics();
  }
}
