package de.thm.arsnova.config;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.core.view.Consistency;

@Configuration
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {

	@Value(value = "${couchbase.host}") private String host;
	@Value(value = "${couchbase.bucketName}") private String bucketName;
	@Value(value = "${couchbase.bucketPassword}") private String bucketPassword;

	@Override
	protected List<String> getBootstrapHosts() {
		return Collections.singletonList(this.host);
	}

	@Override
	protected String getBucketName() {
		return this.bucketName;
	}

	@Override
	protected String getBucketPassword() {
		return this.bucketPassword;
	}

	@Override
	protected Consistency getDefaultConsistency() {
		return Consistency.STRONGLY_CONSISTENT;
	}

}
