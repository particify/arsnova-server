package de.thm.arsnova.controller;

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
	public final String countActiveUsers() {
		return Integer.toString(statisticsService.countActiveUsers());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/statistics/sessioncount", produces = "text/plain")
	@ResponseBody
	public final String countSessions() {
		return Integer.toString(statisticsService.getStatistics().getOpenSessions()
				+ statisticsService.getStatistics().getClosedSessions());
	}
}
