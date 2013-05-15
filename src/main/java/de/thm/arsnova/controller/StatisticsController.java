package de.thm.arsnova.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.thm.arsnova.entities.Statistics;
import de.thm.arsnova.services.IStatisticsService;

@Controller
public class StatisticsController {

	@Autowired
	private IStatisticsService statisticsService;

	@RequestMapping(method = RequestMethod.GET, value = "/statistics")
	@ResponseBody
	public final Statistics getStatistics() {
		return statisticsService.getStatistics();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/statistics/activeusercount", produces = "text/plain")
	@ResponseBody
	public final String countActiveUsers(HttpServletResponse response) {
		response.addHeader("X-Deprecated-API", "1");

		return Integer.toString(statisticsService.countActiveUsers());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/statistics/loggedinusercount", produces = "text/plain")
	@ResponseBody
	public final String countLoggedInUsers(HttpServletResponse response) {
		response.addHeader("X-Deprecated-API", "1");

		return Integer.toString(statisticsService.countLoggedInUsers());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/statistics/sessioncount", produces = "text/plain")
	@ResponseBody
	public final String countSessions(HttpServletResponse response) {
		response.addHeader("X-Deprecated-API", "1");

		return Integer.toString(statisticsService.getStatistics().getOpenSessions()
				+ statisticsService.getStatistics().getClosedSessions());
	}
}
