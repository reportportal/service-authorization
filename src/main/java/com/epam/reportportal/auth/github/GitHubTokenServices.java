package com.epam.reportportal.auth.github;

import com.epam.reportportal.auth.AuthUtils;
import com.epam.ta.reportportal.database.entity.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

/**
 * Token services for GitHub account info with internal ReportPortal's database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class GitHubTokenServices implements ResourceServerTokenServices {

	private final GitHubUserReplicator replicator;

	public GitHubTokenServices(GitHubUserReplicator replicatingPrincipalExtractor) {
		this.replicator = replicatingPrincipalExtractor;
	}

	@Override
	public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
		User user = replicator.replicateUser(accessToken);

		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getId(), "N/A",
				AuthUtils.AS_AUTHORITIES.apply(user.getRole()));
		OAuth2Request request = new OAuth2Request(null, null, null, true, null, null, null, null, null);
		return new OAuth2Authentication(request, token);
	}

	@Override
	public OAuth2AccessToken readAccessToken(String accessToken) {
		throw new UnsupportedOperationException("Not supported: read access token");
	}

}
