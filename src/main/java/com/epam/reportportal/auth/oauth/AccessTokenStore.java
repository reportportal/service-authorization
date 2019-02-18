package com.epam.reportportal.auth.oauth;

import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.dao.OAuth2AccessTokenRepository;
import com.epam.ta.reportportal.entity.user.StoredAccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:pavel_bortnik@epam.com">Pavel Bortnik</a>
 */
@Component(value = "accessTokenStore")
public class AccessTokenStore implements TokenStore {

	@Autowired
	private OAuth2AccessTokenRepository oAuth2AccessTokenRepository;

	private AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();

	@Override
	public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
		return null;
	}

	@Override
	public OAuth2Authentication readAuthentication(String token) {
		return null;
	}

	@Override
	public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
		StoredAccessToken tokenEntity = new StoredAccessToken();
		tokenEntity.setTokenId(token.getValue());
		tokenEntity.setToken(SerializationUtils.serialize(token));
		tokenEntity.setAuthentication(SerializationUtils.serialize(authentication));
		tokenEntity.setAuthenticationId(authenticationKeyGenerator.extractKey(authentication));
		tokenEntity.setUserName(authentication.isClientOnly() ? null : authentication.getName());
		tokenEntity.setUserId(((ReportPortalUser) authentication.getPrincipal()).getUserId());
		tokenEntity.setRefreshToken(null == token.getRefreshToken() ? null : token.getRefreshToken().getValue());
		tokenEntity.setClientId(authentication.getOAuth2Request().getClientId());
		oAuth2AccessTokenRepository.save(tokenEntity);
	}

	@Override
	public OAuth2AccessToken readAccessToken(String tokenValue) {
		StoredAccessToken token = oAuth2AccessTokenRepository.findByTokenId(tokenValue);
		if (token == null) {
			return null; //let spring security handle the invalid token
		}
		return SerializationUtils.deserialize(token.getToken());
	}

	@Override
	public void removeAccessToken(OAuth2AccessToken token) {
		StoredAccessToken accessToken = oAuth2AccessTokenRepository.findByTokenId(token.getValue());
		if (accessToken != null) {
			oAuth2AccessTokenRepository.delete(accessToken);
		}
	}

	@Override
	public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
		//not used
	}

	@Override
	public OAuth2RefreshToken readRefreshToken(String tokenValue) {
		//not used
		return null;
	}

	@Override
	public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
		//not used
		return null;
	}

	@Override
	public void removeRefreshToken(OAuth2RefreshToken token) {
		//not used
	}

	@Override
	public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
		oAuth2AccessTokenRepository.delete(oAuth2AccessTokenRepository.findByRefreshToken(refreshToken.getValue()));
	}

	@Override
	public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
		StoredAccessToken token = oAuth2AccessTokenRepository.findByAuthenticationId(authenticationKeyGenerator.extractKey(authentication));
		return token == null ? null : SerializationUtils.deserialize(token.getToken());
	}

	@Override
	public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
		return oAuth2AccessTokenRepository.findByClientId(clientId).map(this::extractAccessToken).collect(Collectors.toList());
	}

	@Override
	public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String userName) {
		return oAuth2AccessTokenRepository.findByClientIdAndUserName(clientId, userName)
				.map(this::extractAccessToken)
				.collect(Collectors.toList());
	}

	private OAuth2AccessToken extractAccessToken(StoredAccessToken token) {
		return (OAuth2AccessToken) SerializationUtils.deserialize(token.getToken());
	}
}
