package com.epam.reportportal.auth;

import com.epam.reportportal.auth.store.AuthConfigRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public abstract class EnableableAuthProvider implements AuthenticationProvider {

	protected final AuthConfigRepository authConfigRepository;

	protected EnableableAuthProvider(AuthConfigRepository authConfigRepository) {
		this.authConfigRepository = authConfigRepository;
	}

	protected abstract boolean isEnabled();

	protected abstract AuthenticationProvider getDelegate();

	@Override
	public final Authentication authenticate(Authentication authentication) throws AuthenticationException {
		return isEnabled() ? getDelegate().authenticate(authentication) : null;
	}

	@Override
	public final boolean supports(Class<?> authentication) {
		return isEnabled() && getDelegate().supports(authentication);
	}

}
