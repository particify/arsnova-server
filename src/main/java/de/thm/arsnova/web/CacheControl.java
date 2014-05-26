package de.thm.arsnova.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheControl {
	enum Policy {
		NO_CACHE("no-cache"),
		NO_STORE("no-store"),
		PRIVATE("private"),
		PUBLIC("public");

		private Policy() {
			this.policyString = null;
		}

		private Policy(String policyString) {
			this.policyString = policyString;
		}

		public String getPolicyString() {
			return this.policyString;
		}

		private final String policyString;
	}

	boolean noCache() default false;

	int maxAge() default 0;
	Policy[] policy() default {};
}
