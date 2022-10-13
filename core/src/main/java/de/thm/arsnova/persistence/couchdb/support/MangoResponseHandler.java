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

package de.thm.arsnova.persistence.couchdb.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.ektorp.http.HttpResponse;
import org.ektorp.http.StdResponseHandler;
import org.ektorp.util.Assert;

public class MangoResponseHandler<T> extends StdResponseHandler<List<T>> {

  private MangoQueryResultParser<T> parser;
  private String bookmark;

  public MangoResponseHandler(final Class<T> docType, final ObjectMapper om) {
    Assert.notNull(om, "ObjectMapper may not be null");
    Assert.notNull(docType, "docType may not be null");
    parser = new MangoQueryResultParser<T>(docType, om);
  }

  public MangoResponseHandler(final Class<T> docType, final ObjectMapper om,
      final boolean ignoreNotFound) {
    Assert.notNull(om, "ObjectMapper may not be null");
    Assert.notNull(docType, "docType may not be null");
    parser = new MangoQueryResultParser<T>(docType, om);
  }

  public MangoResponseHandler(final String propertyName, final Class<T> propertyType, final ObjectMapper om) {
    Assert.notNull(om, "ObjectMapper may not be null");
    Assert.notNull(propertyType, "propertyType may not be null");
    Assert.notNull(propertyName, "propertyName may not be null");
    parser = new MangoQueryResultParser<T>(propertyName, propertyType, om);
  }

  @Override
  public List<T> success(final HttpResponse hr) throws Exception {
    parser.parseResult(hr.getContent());
    bookmark = parser.getBookmark();

    return parser.getDocs();
  }

  public String getBookmark() {
    return bookmark;
  }
}
