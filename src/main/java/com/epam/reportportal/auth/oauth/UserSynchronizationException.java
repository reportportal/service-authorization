package com.epam.reportportal.auth.oauth;

import org.springframework.security.core.AuthenticationException;

/**
 * In case of user synchronization has failed
 *
 * @author Andrei Varabyeu
 */
public class UserSynchronizationException extends AuthenticationException {

	public UserSynchronizationException(String msg) {
		super(msg);
	}
}