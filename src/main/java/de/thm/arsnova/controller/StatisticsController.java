/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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
package de.thm.arsnova.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.entities.Statistics;
import de.thm.arsnova.services.IStatisticsService;
import de.thm.arsnova.web.CacheControl;
import de.thm.arsnova.web.DeprecatedApi;

/**
 * Allows retrieval of several statistics such as the number of active users.
 */
@RestController
public class StatisticsController extends AbstractController {

	@Autowired
	private IStatisticsService statisticsService;

	@RequestMapping(method = RequestMethod.GET, value = "/statistics")
	@CacheControl(maxAge = 60, policy = CacheControl.Policy.PUBLIC)
	public Statistics getStatistics() {
		return statisticsService.getStatistics();
	}

	@DeprecatedApi
	@RequestMapping(method = RequestMethod.GET, value = "/statistics/activeusercount", produces = "text/plain")
	public String countActiveUsers() {
		return Integer.toString(statisticsService.getStatistics().getActiveUsers());
	}

	@DeprecatedApi
	@RequestMapping(method = RequestMethod.GET, value = "/statistics/loggedinusercount", produces = "text/plain")
	public String countLoggedInUsers() {
		return Integer.toString(statisticsService.getStatistics().getLoggedinUsers());
	}

	@DeprecatedApi
	@RequestMapping(method = RequestMethod.GET, value = "/statistics/sessioncount", produces = "text/plain")
	public String countSessions() {
		return Integer.toString(statisticsService.getStatistics().getOpenSessions()
				+ statisticsService.getStatistics().getClosedSessions());
	}
}
