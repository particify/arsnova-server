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

package net.particify.arsnova.core.web;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import net.particify.arsnova.core.service.StatusService;

/**
 * Blocks API requests while maintenance reasons are active.
 *
 * @author Daniel Gerhardt
 */
@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {
  private StatusService statusService;

  public MaintenanceModeFilter(final StatusService statusService) {
    this.statusService = statusService;
  }

  @Override
  protected void doFilterInternal(final HttpServletRequest httpServletRequest,
      final HttpServletResponse httpServletResponse,
      final FilterChain filterChain)
      throws ServletException, IOException {
    if (statusService.isMaintenanceActive()) {
      httpServletResponse.setStatus(503);
      return;
    }
    filterChain.doFilter(httpServletRequest, httpServletResponse);
  }
}
