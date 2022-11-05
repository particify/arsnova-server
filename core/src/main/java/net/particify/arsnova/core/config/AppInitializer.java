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

package net.particify.arsnova.core.config;

import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class AppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
  private static final int UPLOAD_MAX_FILE_SIZE = 50 * 1024;
  private static final int UPLOAD_MAX_REQUST_SIZE = UPLOAD_MAX_FILE_SIZE;
  private static final int UPLOAD_FILE_SIZE_THRESHOLD = UPLOAD_MAX_FILE_SIZE;
  private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

  @Override
  protected Class<?>[] getRootConfigClasses() {
    return new Class[] {
        AppConfig.class,
        PersistenceConfig.class,
        SecurityConfig.class,
        RabbitConfig.class,
        TaskExecutorConfig.class,
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
    final DelegatingFilterProxy webMvcMetricsFilter = new DelegatingFilterProxy("webMvcMetricsFilterOverride");
    final DelegatingFilterProxy corsFilter = new DelegatingFilterProxy("corsFilter");
    final DelegatingFilterProxy maintenanceModeFilter = new DelegatingFilterProxy("maintenanceModeFilter");

    return new Filter[] {
        webMvcMetricsFilter,
        characterEncodingFilter,
        corsFilter,
        maintenanceModeFilter
    };
  }

  @Override
  protected void customizeRegistration(final ServletRegistration.Dynamic registration) {
    registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
    final MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
        TMP_DIR,
        UPLOAD_MAX_FILE_SIZE,
        UPLOAD_MAX_REQUST_SIZE,
        UPLOAD_FILE_SIZE_THRESHOLD);
    registration.setMultipartConfig(multipartConfigElement);
  }

  @Override
  protected String getServletName() {
    return "arsnova";
  }
}
