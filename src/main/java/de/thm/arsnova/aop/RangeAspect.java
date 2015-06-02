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
package de.thm.arsnova.aop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.thm.arsnova.controller.PaginationController;

@Component
@Aspect
@Profile("!test")
public class RangeAspect {
	@Autowired
	private HttpServletRequest request;

	private final Pattern rangePattern = Pattern.compile("^items=([0-9]+)-([0-9]+)?$");

	private static final Logger logger = LoggerFactory.getLogger(RangeAspect.class);

	/** Sets start and end parameters based on range header
	 *
	 * @param controller
	 */
	@Before("execution(* de.thm.arsnova.controller.*.*(..)) && this(controller) && @annotation(de.thm.arsnova.web.Pagination)")
	public void parsePaginationRange(final PaginationController controller) {
		String rangeHeader = request.getHeader("Range");
		Matcher matcher = null;
		if (rangeHeader != null) {
			matcher = rangePattern.matcher(rangeHeader);
		}

		if (matcher != null && matcher.matches()) {
			int start = matcher.group(1) != null ? Integer.valueOf(matcher.group(1)) : -1;
			int end = matcher.group(2) != null ? Integer.valueOf(matcher.group(2)) : -1;
			logger.debug("Pagination: {}-{}", start, end);
			controller.setRange(start, end);
		} else {
			controller.setRange(-1, -1);
		}
	}
}
