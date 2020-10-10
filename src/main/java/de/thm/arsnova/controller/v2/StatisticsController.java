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

package de.thm.arsnova.controller.v2;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.controller.AbstractController;
import de.thm.arsnova.model.migration.v2.Statistics;
import de.thm.arsnova.service.StatisticsService;
import de.thm.arsnova.service.UserService;
import de.thm.arsnova.web.CacheControl;
import de.thm.arsnova.web.DeprecatedApi;

/**
 * Allows retrieval of several statistics such as the number of active users.
 */
@RestController("v2StatisticsController")
@Api(value = "/statistics", description = "Statistics API")
@RequestMapping("/v2/statistics")
public class StatisticsController extends AbstractController {

	@Autowired
	private StatisticsService statisticsService;

	@Autowired
	private UserService userService;

	@ApiOperation(value = "Retrieves global statistics",
			nickname = "getStatistics")
	@GetMapping("/")
	@CacheControl(maxAge = 60, policy = CacheControl.Policy.PUBLIC)
	public Statistics getStatistics() {
		return new Statistics(statisticsService.getStatistics());
	}

	@ApiOperation(value = "Retrieves the amount of all active users",
			nickname = "countActiveUsers")
	@DeprecatedApi
	@Deprecated
	@GetMapping(value = "/activeusercount", produces = MediaType.TEXT_PLAIN_VALUE)
	public String countActiveUsers() {
		return String.valueOf(userService.loggedInUsers());
	}

	@ApiOperation(value = "Retrieves the amount of all currently logged in users",
			nickname = "countLoggedInUsers")
	@DeprecatedApi
	@Deprecated
	@GetMapping(value = "/loggedinusercount", produces = MediaType.TEXT_PLAIN_VALUE)
	public String countLoggedInUsers() {
		return String.valueOf(userService.loggedInUsers());
	}

	@ApiOperation(value = "Retrieves the total amount of all sessions",
			nickname = "countSessions")
	@DeprecatedApi
	@Deprecated
	@GetMapping(value = "/sessioncount", produces = MediaType.TEXT_PLAIN_VALUE)
	public String countSessions() {
		return String.valueOf(statisticsService.getStatistics().getRoom().getTotalCount());
	}
}
