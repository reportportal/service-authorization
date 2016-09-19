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