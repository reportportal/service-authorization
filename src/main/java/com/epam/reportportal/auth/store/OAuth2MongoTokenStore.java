/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/epam/ReportPortal
 * 
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */ 
package com.epam.reportportal.auth.store;

import com.epam.reportportal.auth.store.entity.OAuth2AccessTokenEntity;
import com.epam.reportportal.auth.store.entity.OAuth2RefreshTokenEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.TokenStore;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrei Varabyeu
 */
public class OAuth2MongoTokenStore implements TokenStore {

	@Autowired
	private OAuth2AccessTokenRepository oAuth2AccessTokenRepository;

	@Autowired
	private OAuth2RefreshTokenRepository oAuth2RefreshTokenRepository;

	private AuthenticationKeyGenerator authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();

	@Override
	public OAuth2Authentication readAuthentication(OAuth2AccessToken token) {
		return readAuthentication(token.getValue());
	}

	@Override
	public OAuth2Authentication readAuthentication(String tokenId) {
		return SerializationUtils.deserialize(oAuth2AccessTokenRepository.findByTokenId(tokenId).getAuthentication());
	}

	@Override
	public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
		OAuth2AccessTokenEntity tokenEntity = new OAuth2AccessTokenEntity();
		tokenEntity.setTokenId(token.getValue());
		tokenEntity.setToken(SerializationUtils.serialize(token));
		tokenEntity.setAuthentication(SerializationUtils.serialize(authentication));
		tokenEntity.setAuthenticationId(authenticationKeyGenerator.extractKey(authentication));
		tokenEntity.setUserName(authentication.isClientOnly() ? null : authentication.getName());
		tokenEntity.setRefreshToken(null == token.getRefreshToken() ? null : token.getRefreshToken().getValue());
		tokenEntity.setClientId(authentication.getOAuth2Request().getClientId());

		oAuth2AccessTokenRepository.save(tokenEntity);
	}

	@Override
	public OAuth2AccessToken readAccessToken(String tokenValue) {
		OAuth2AccessTokenEntity token = oAuth2AccessTokenRepository.findByTokenId(tokenValue);
		if (token == null) {
			return null; //let spring security handle the invalid token
		}
		return SerializationUtils.deserialize(token.getToken());
	}

	@Override
	public void removeAccessToken(OAuth2AccessToken token) {
		OAuth2AccessTokenEntity accessToken = oAuth2AccessTokenRepository.findByTokenId(token.getValue());
		if (accessToken != null) {
			oAuth2AccessTokenRepository.delete(accessToken);
		}
	}

	@Override
	public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
		OAuth2RefreshTokenEntity refreshEntity = new OAuth2RefreshTokenEntity();
		refreshEntity.setAuthentication(SerializationUtils.serialize(authentication));
		refreshEntity.setTokenId(refreshToken.getValue());
		refreshEntity.setoAuth2RefreshToken(SerializationUtils.serialize(refreshToken));
		oAuth2RefreshTokenRepository.save(refreshEntity);
	}

	@Override
	public OAuth2RefreshToken readRefreshToken(String tokenValue) {
		return SerializationUtils.deserialize(oAuth2RefreshTokenRepository.findByTokenId(tokenValue).getoAuth2RefreshToken());
	}

	@Override
	public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
		return SerializationUtils.deserialize(oAuth2RefreshTokenRepository.findByTokenId(token.getValue()).getAuthentication());
	}

	@Override
	public void removeRefreshToken(OAuth2RefreshToken token) {
		oAuth2RefreshTokenRepository.delete(oAuth2RefreshTokenRepository.findByTokenId(token.getValue()));
	}

	@Override
	public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
		oAuth2AccessTokenRepository.delete(oAuth2AccessTokenRepository.findByRefreshToken(refreshToken.getValue()));
	}

	@Override
	public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
		OAuth2AccessTokenEntity token = oAuth2AccessTokenRepository
				.findByAuthenticationId(authenticationKeyGenerator.extractKey(authentication));
		return token == null ? null : SerializationUtils.deserialize(token.getToken());
	}

	@Override
	public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
		List<OAuth2AccessTokenEntity> tokens = oAuth2AccessTokenRepository.findByClientId(clientId);
		return extractAccessTokens(tokens);
	}

	@Override
	public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String userName) {
		List<OAuth2AccessTokenEntity> tokens = oAuth2AccessTokenRepository.findByClientIdAndUserName(clientId, userName);
		return extractAccessTokens(tokens);
	}

	private Collection<OAuth2AccessToken> extractAccessTokens(List<OAuth2AccessTokenEntity> tokens) {
		return tokens.stream().map(entity -> (OAuth2AccessToken) SerializationUtils.deserialize(entity.getToken()))
				.collect(Collectors.toList());
	}
}