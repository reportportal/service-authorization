package com.epam.reportportal.auth;

import com.epam.ta.reportportal.database.entity.user.UserRole;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author Andrei Varabyeu
 */
public class AuthUtilsTest {

	@Test
	public void testAuthoritiesConverter() {
		Collection<GrantedAuthority> grantedAuthorities = Collections.singletonList(UserRole.USER).stream().map(AuthUtils.AS_AUTHORITIES)
				.collect(Collectors.toList()).get(0);
		Assert.assertThat("Incorrect authority conversion", grantedAuthorities.iterator().next().getAuthority(),
				Matchers.is(UserRole.USER.getAuthority()));

	}
}
