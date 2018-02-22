/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.Statistics;
import de.thm.arsnova.persistance.StatisticsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Performs all statistics related operations. To reduce pressure on the database, data is cached for a fixed amount of
 * time.
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {
	private StatisticsRepository statisticsRepository;

	private UserService userService;

	public StatisticsServiceImpl(
			StatisticsRepository repository,
			UserService userService) {
		this.statisticsRepository = repository;
		this.userService = userService;
	}

	private Statistics statistics = new Statistics();

	@Scheduled(initialDelay = 0, fixedRate = 10000)
	private void refreshStatistics() {
		statistics = loadStatistics();
	}

	@Cacheable("statistics")
	private Statistics loadStatistics() {
		return statisticsRepository.getStatistics();
	}

	@Override
	public Statistics getStatistics() {
		statistics.setActiveUsers(userService.loggedInUsers());
		return statistics;
	}
}
