/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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
import de.thm.arsnova.services.SessionService;
import org.ektorp.CouchDbConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.List;

public class CouchDbMotdRepository extends CouchDbCrudRepository<Motd> implements MotdRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbMotdRepository.class);

	@Autowired
	private SessionService sessionService;

	public CouchDbMotdRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Motd.class, db, "by_sessionkey", createIfNotExists);
	}

	@Override
	public List<Motd> findGlobalForAdmin() {
		return find("by_audience_for_global", null);
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "'all'")
	public List<Motd> findGlobalForAll() {
		return find("by_audience_for_global", "all");
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "'loggedIn'")
	public List<Motd> findGlobalForLoggedIn() {
		return find("by_audience_for_global", "loggedIn");
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "'tutors'")
	public List<Motd> findGlobalForTutors() {
		final List<Motd> union = new ArrayList<>();
		union.addAll(find("by_audience_for_global", "loggedIn"));
		union.addAll(find("by_audience_for_global", "tutors"));

		return union;
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "'students'")
	public List<Motd> findForStudents() {
		final List<Motd> union = new ArrayList<>();
		union.addAll(find("by_audience_for_global", "loggedIn"));
		union.addAll(find("by_audience_for_global", "students"));

		return union;
	}

	@Override
	@Cacheable(cacheNames = "motds", key = "('session').concat(#p0)")
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

	@Override
	@CacheEvict(cacheNames = "motds", key = "#p0.audience.concat(#p0.sessionkey)")
	public Motd save(final Motd motd) {
		final String id = motd.getId();
		final String rev = motd.getRevision();

		if (null != id) {
			Motd oldMotd = get(id);
			motd.setMotdkey(oldMotd.getMotdkey());
			update(motd);
		} else {
			motd.setMotdkey(sessionService.generateKey());
			add(motd);
		}

		return motd;
	}

	/* TODO: Redundant -> remove. Move cache handling to service layer. */
	@Override
	@CacheEvict(cacheNames = "motds", key = "#p0.audience.concat(#p0.sessionkey)")
	public void delete(final Motd motd) {
		db.delete(motd);
	}
}
