package de.thm.arsnova.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.entities.Statistics;
import de.thm.arsnova.services.IStatisticsService;
import de.thm.arsnova.web.CacheControl;
import de.thm.arsnova.web.DeprecatedApi;

@RestController
public class StatisticsController extends AbstractController {

	@Autowired
	private IStatisticsService statisticsService;

	@RequestMapping(method = RequestMethod.GET, value = "/statistics")
	@CacheControl(maxAge = 60, policy = CacheControl.Policy.PUBLIC)
	public final Statistics getStatistics() {
		return statisticsService.getStatistics();
	}

	@DeprecatedApi
	@RequestMapping(method = RequestMethod.GET, value = "/statistics/activeusercount", produces = "text/plain")
	public final String countActiveUsers() {
		return Integer.toString(statisticsService.countActiveUsers());
	}

	@DeprecatedApi
	@RequestMapping(method = RequestMethod.GET, value = "/statistics/loggedinusercount", produces = "text/plain")
	public final String countLoggedInUsers() {
		return Integer.toString(statisticsService.countLoggedInUsers());
	}

	@DeprecatedApi
	@RequestMapping(method = RequestMethod.GET, value = "/statistics/sessioncount", produces = "text/plain")
	public final String countSessions() {
		return Integer.toString(statisticsService.getStatistics().getOpenSessions()
				+ statisticsService.getStatistics().getClosedSessions());
	}
}
