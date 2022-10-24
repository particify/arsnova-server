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

package de.thm.arsnova.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Statistics;
import de.thm.arsnova.persistence.StatisticsRepository;

/**
 * Performs all statistics related operations. To reduce pressure on the database, data is cached for a fixed amount of
 * time.
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {
  private static final long STATISTICS_REFRESH_INTERVAL_MS = 30000;
  private static final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);
  private StatisticsRepository statisticsRepository;

  public StatisticsServiceImpl(final StatisticsRepository repository) {
    this.statisticsRepository = repository;
  }

  @Scheduled(initialDelay = 0, fixedRate = STATISTICS_REFRESH_INTERVAL_MS)
  @CacheEvict(value = "system", key = "'statistics'")
  private void clearCachedStatistics() {
    logger.trace("Evicting statistics from cache.");
  }

  @Override
  @Cacheable(value = "system", key = "'statistics'")
  public Statistics getStatistics() {
    logger.debug("Loading statistics (uncached).");
    return statisticsRepository.getStatistics();
  }
}
