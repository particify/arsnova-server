package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.model.serialization.View;
import org.springframework.core.style.ToStringCreator;

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

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("loginId", loginId)
				.append("password", password)
				.toString();
	}
}
