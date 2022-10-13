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

package de.thm.arsnova.model.serialization;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.ektorp.CouchDbConnector;
import org.ektorp.impl.StdObjectMapperFactory;

public class CouchDbObjectMapperFactory extends StdObjectMapperFactory {
  public ObjectMapper createObjectMapper(final CouchDbConnector connector) {
    final ObjectMapper om = super.createObjectMapper(connector);
    om.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
    om.setConfig(om.getSerializationConfig().withView(View.Persistence.class));
    om.registerModule(new JavaTimeModule());
    om.registerModule(new CouchDbDocumentModule());

    return om;
  }
}
