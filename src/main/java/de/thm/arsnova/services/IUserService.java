package de.thm.arsnova.services;

import org.springframework.security.core.Authentication;

import de.thm.arsnova.entities.User;

public interface IUserService {
	User getUser(Authentication authentication);
}
