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
package de.thm.arsnova.websocket;

import de.thm.arsnova.events.ArsnovaEvent;
import de.thm.arsnova.events.ArsnovaEventVisitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * An external Listener is required because otherwise the event methods are not called through a Spring proxy.
 * This would result in Spring method annotations not working.
 */
@Component
public class ArsnovaSocketioServerListener implements ApplicationListener<ArsnovaEvent> {

	@Autowired
	private ArsnovaSocketioServer socketioServer;

	@Override
	public void onApplicationEvent(ArsnovaEvent event) {
		event.accept((ArsnovaEventVisitor) socketioServer);
	}

}
