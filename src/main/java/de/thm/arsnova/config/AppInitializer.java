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

package de.thm.arsnova.config;

import javax.servlet.Filter;
import javax.servlet.ServletRegistration;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class[] {
				AppConfig.class,
				PersistenceConfig.class,
				SecurityConfig.class,
				WebSocketConfig.class,
		};
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class[0];
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] {
				"/"
		};
	}

	@Override
	protected Filter[] getServletFilters() {
		final CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter("UTF-8");
		final DelegatingFilterProxy corsFilter = new DelegatingFilterProxy("corsFilter");
		final DelegatingFilterProxy maintenanceModeFilter = new DelegatingFilterProxy("maintenanceModeFilter");
		final DelegatingFilterProxy v2ContentTypeOverrideFilter = new DelegatingFilterProxy("v2ContentTypeOverrideFilter");

		return new Filter[] {
				characterEncodingFilter,
				corsFilter,
				maintenanceModeFilter,
				v2ContentTypeOverrideFilter
		};
	}

	@Override
	protected void customizeRegistration(final ServletRegistration.Dynamic registration) {
		registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
	}

	@Override
	protected String getServletName() {
		return "arsnova";
	}
}
