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
package de.thm.arsnova.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of maintenance reasons registered by other components. While at least one maintenance reason is active,
 * API access is blocked by {@link de.thm.arsnova.web.MaintenanceModeFilter}.
 *
 * @author Daniel Gerhardt
 */
@Service
public class StatusServiceImpl implements StatusService {
	private final Map<Class<?>, String> maintenanceReasons = new HashMap<>();

	@Override
	public void putMaintenanceReason(final Class<?> type, final String reason) {
		maintenanceReasons.put(type, reason);
	}

	@Override
	public void removeMaintenanceReason(final Class<?> type) {
		maintenanceReasons.remove(type);
	}

	@Override
	public Map<Class<?>, String> getMaintenanceReasons() {
		return maintenanceReasons;
	}

	@Override
	public boolean isMaintenanceActive() {
		return !maintenanceReasons.isEmpty();
	}
}
