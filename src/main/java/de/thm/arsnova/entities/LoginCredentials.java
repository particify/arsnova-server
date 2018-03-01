package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

public class LoginCredentials {
	private String loginId;
	private String password;

	public String getLoginId() {
		return loginId;
	}

	@JsonView(View.Public.class)
	public void setLoginId(final String loginId) {
		this.loginId = loginId;
	}

	public String getPassword() {
		return password;
	}

	@JsonView(View.Public.class)
	public void setPassword(final String password) {
		this.password = password;
	}
}
