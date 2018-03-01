package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

public class ClientAuthentication {
	private String userId;
	private String loginId;
	private UserProfile.AuthProvider authProvider;
	private String token;

	public ClientAuthentication(final String userId, final String loginId, final UserProfile.AuthProvider authProvider,
			final String token) {
		this.userId = userId;
		this.loginId = loginId;
		this.authProvider = authProvider;
		this.token = token;
	}

	@JsonView(View.Public.class)
	public String getUserId() {
		return userId;
	}

	@JsonView(View.Public.class)
	public String getLoginId() {
		return loginId;
	}

	@JsonView(View.Public.class)
	public UserProfile.AuthProvider getAuthProvider() {
		return authProvider;
	}

	@JsonView(View.Public.class)
	public String getToken() {
		return token;
	}
}
