package de.thm.arsnova.persistance.couchdb.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ektorp.http.HttpResponse;
import org.ektorp.http.StdResponseHandler;
import org.ektorp.util.Assert;

import java.util.List;

public class MangoResponseHandler<T> extends StdResponseHandler<List<T>> {

	private MangoQueryResultParser<T> parser;

	public MangoResponseHandler(Class<T> docType, ObjectMapper om) {
		Assert.notNull(om, "ObjectMapper may not be null");
		Assert.notNull(docType, "docType may not be null");
		parser = new MangoQueryResultParser<T>(docType, om);
	}

	public MangoResponseHandler(Class<T> docType, ObjectMapper om,
										  boolean ignoreNotFound) {
		Assert.notNull(om, "ObjectMapper may not be null");
		Assert.notNull(docType, "docType may not be null");
		parser = new MangoQueryResultParser<T>(docType, om);
	}

	@Override
	public List<T> success(HttpResponse hr) throws Exception {
		parser.parseResult(hr.getContent());
		return parser.getDocs();
	}
}
