/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * Implementation of custom token store. It is used to store unexpired api token
 * in the database. It is a dirty hack to use it with test frameworks.
 * It just stores an {@link AccessTokenStore} and retrieves it from db.
 *
 * @author <a href="mailto:pavel_bortnik@epam.com">Pavel Bortnik</a>
 */
@Component(value = "accessTokenStore")
public class AccessTokenStore implements TokenStore {

	@Autowired
	private OAuth2AccessTokenRepository oAuth2AccessTokenRepository;

	private AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();

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
	public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
		throw new UnsupportedOperationException("Access token store doesn't use this.");
	}

	@Override
	public OAuth2Authentication readAuthentication(String token) {
		throw new UnsupportedOperationException("Access token store doesn't use this.");
	}

	@Override
	public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
		throw new UnsupportedOperationException("Access token store doesn't use this.");
	}

	@Override
	public OAuth2RefreshToken readRefreshToken(String tokenValue) {
		throw new UnsupportedOperationException("Access token store doesn't use this.");
	}

	@Override
	public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
		throw new UnsupportedOperationException("Access token store doesn't use this.");
	}

	@Override
	public void removeRefreshToken(OAuth2RefreshToken token) {
		throw new UnsupportedOperationException("Access token store doesn't use this.");
	}

	@Override
	public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
		throw new UnsupportedOperationException("Access token store doesn't use this.");
	}

	@Override
	public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
		StoredAccessToken token = oAuth2AccessTokenRepository.findByAuthenticationId(authenticationKeyGenerator.extractKey(authentication));
		return token == null ? null : SerializationUtils.deserialize(token.getToken());
	}

	@Override
	public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
		throw new UnsupportedOperationException("Access token store doesn't use this.");
	}

	@Override
	public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String userName) {
		throw new UnsupportedOperationException("Access token store doesn't use this.");
	}
}
