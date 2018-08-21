package de.thm.arsnova.config;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;
import javax.servlet.ServletRegistration;

public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class[] {
				AppConfig.class,
				PersistenceConfig.class,
				SecurityConfig.class
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
		CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter("UTF-8");
		DelegatingFilterProxy corsFilter = new DelegatingFilterProxy("corsFilter");
		DelegatingFilterProxy maintenanceModeFilter = new DelegatingFilterProxy("maintenanceModeFilter");
		DelegatingFilterProxy v2ContentTypeOverrideFilter = new DelegatingFilterProxy("v2ContentTypeOverrideFilter");

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
