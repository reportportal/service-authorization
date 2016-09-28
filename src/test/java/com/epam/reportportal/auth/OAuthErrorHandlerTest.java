/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
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
package com.epam.reportportal.auth;

import com.epam.ta.reportportal.commons.ExceptionMappings;
import com.epam.ta.reportportal.commons.exception.rest.DefaultErrorResolver;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

import java.util.Map;

/**
 * @author Andrei Varabyeu
 */
public class OAuthErrorHandlerTest {

	private OAuthErrorHandler errorHandler = new OAuthErrorHandler(new DefaultErrorResolver(ExceptionMappings.DEFAULT_MAPPING));

	@Test
	public void testOAuthException() throws Exception {
		String msg = "some exception!";
		ResponseEntity<OAuth2Exception> translate = errorHandler.translate(new InvalidTokenException(msg));
		Map<String, String> additionalInformation = translate.getBody().getAdditionalInformation();

		Assert.assertThat("Incorrect exception conversion", additionalInformation,
				Matchers.hasEntry("message", translate.getBody().getMessage()));

	}

	@Test
	public void testNotOauthException() throws Exception {

		String msg = "some exception!";

		ResponseEntity<OAuth2Exception> translate = errorHandler.translate(new RuntimeException(msg));
		Map<String, String> additionalInformation = translate.getBody().getAdditionalInformation();
		Assert.assertThat("Incorrect exception conversion", additionalInformation,
				Matchers.hasEntry("message", translate.getBody().getMessage()));

	}

}