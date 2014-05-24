package de.thm.arsnova.controller;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.entities.Statistics;
import de.thm.arsnova.services.IStatisticsService;

@RestController
public class StatisticsController extends AbstractController {

	@Autowired
	private IStatisticsService statisticsService;

	@RequestMapping(method = RequestMethod.GET, value = "/statistics")
	public final Statistics getStatistics() {
		return statisticsService.getStatistics();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/astatistics")
	public final Callable<Statistics> getAsyncStatistics() {
		return new Callable<Statistics> () {

			@Override
			public Statistics call() throws Exception {
				return statisticsService.getStatistics();
			}
		};
	}

	@RequestMapping(method = RequestMethod.GET, value = "/statistics/activeusercount", produces = "text/plain")
	public final String countActiveUsers(HttpServletResponse response) {
		response.addHeader(X_DEPRECATED_API, "1");

		return Integer.toString(statisticsService.countActiveUsers());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/statistics/loggedinusercount", produces = "text/plain")
	public final String countLoggedInUsers(HttpServletResponse response) {
		response.addHeader(X_DEPRECATED_API, "1");

		return Integer.toString(statisticsService.countLoggedInUsers());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/statistics/sessioncount", produces = "text/plain")
	public final String countSessions(HttpServletResponse response) {
		response.addHeader(X_DEPRECATED_API, "1");

		return Integer.toString(statisticsService.getStatistics().getOpenSessions()
				+ statisticsService.getStatistics().getClosedSessions());
	}
}
