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
package de.thm.arsnova.aop;

import de.thm.arsnova.PaginationListDecorator;
import de.thm.arsnova.controller.PaginationController;
import de.thm.arsnova.services.ResponseProviderService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An aspect which parses requests for pagination parameters in a "Range" header and adds a "Content-Range" header to
 * the response. It only applies to methods of {@link PaginationController}s annotated with
 * {@link de.thm.arsnova.web.Pagination} which return a {@link List}.
 */
@Component
@Aspect
@Profile("!test")
public class RangeAspect {

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private ResponseProviderService responseProviderService;

	private final Pattern rangePattern = Pattern.compile("^items=([0-9]+)-([0-9]+)?$");

	private static final Logger logger = LoggerFactory.getLogger(RangeAspect.class);

	/** Sets start and end parameters based on request's range header and sets content range header for the response.
	 */
	@Around("execution(java.util.List+ de.thm.arsnova.controller.*.*(..)) && this(controller) && @annotation(de.thm.arsnova.web.Pagination)")
	public Object handlePaginationRange(ProceedingJoinPoint pjp, final PaginationController controller) throws Throwable {
		logger.debug("handlePaginationRange");
		String rangeHeader = request.getHeader("Range");
		Matcher matcher = null;
		int start = -1;
		int end = -1;

		if (rangeHeader != null) {
			matcher = rangePattern.matcher(rangeHeader);
		}

		if (matcher != null && matcher.matches()) {
			start = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : -1;
			end = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : -1;
			logger.debug("Pagination: {}-{}", start, end);
		}
		controller.setRange(start, end);

		List<?> list = (List<?>) pjp.proceed();

		if (list != null && matcher != null && matcher.matches()) {
			int totalSize = -1;
			if (list instanceof PaginationListDecorator) {
				PaginationListDecorator<?> pl = (PaginationListDecorator<?>) list;
				totalSize = pl.getTotalSize();
			}

			/* Header format: "items <start>-<end>/<total>"
			 *
			 * The value for end is calculated since the result list
			 * could be shorter than requested.
			 */
			String rangeStr = String.format("items %d-%d/%d", start, start + list.size() - 1, totalSize);
			HttpServletResponse response = responseProviderService.getResponse();
			response.addHeader("Content-Range", rangeStr);
		}

		return list;
	}
}
