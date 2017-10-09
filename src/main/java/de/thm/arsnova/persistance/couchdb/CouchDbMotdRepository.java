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
package de.thm.arsnova.persistance.couchdb;

import de.thm.arsnova.entities.Motd;
import de.thm.arsnova.persistance.MotdRepository;
import org.ektorp.CouchDbConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CouchDbMotdRepository extends CouchDbCrudRepository<Motd> implements MotdRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbMotdRepository.class);

	public CouchDbMotdRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Motd.class, db, "by_sessionkey", createIfNotExists);
	}

	@Override
	public List<Motd> findGlobalForAdmin() {
		return find("by_audience_for_global", null);
	}

	@Override
	public List<Motd> findGlobalForAll() {
		return find("by_audience_for_global", "all");
	}

	@Override
	public List<Motd> findGlobalForLoggedIn() {
		return find("by_audience_for_global", "loggedIn");
	}

	@Override
	public List<Motd> findGlobalForTutors() {
		final List<Motd> union = new ArrayList<>();
		union.addAll(find("by_audience_for_global", "loggedIn"));
		union.addAll(find("by_audience_for_global", "tutors"));

		return union;
	}

	@Override
	public List<Motd> findForStudents() {
		final List<Motd> union = new ArrayList<>();
		union.addAll(find("by_audience_for_global", "loggedIn"));
		union.addAll(find("by_audience_for_global", "students"));

		return union;
	}

	@Override
	public List<Motd> findBySessionKey(final String sessionkey) {
		return find("by_sessionkey", sessionkey);
	}

	private List<Motd> find(final String viewName, final String key) {
		return queryView(viewName, key);
	}

	@Override
	public Motd findByKey(final String key) {
		final List<Motd> motd = queryView("by_motdkey", key);

		return motd.get(0);
	}
}
