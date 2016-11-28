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

import com.epam.ta.reportportal.commons.exception.rest.ErrorResolver;
import com.epam.ta.reportportal.commons.exception.rest.RestError;
import com.epam.ta.reportportal.ws.model.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.error.DefaultWebResponseExceptionTranslator;

/**
 * Bridge between {@link ErrorResolver} and {@link org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator}
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class OAuthErrorHandler extends DefaultWebResponseExceptionTranslator {

	private final ErrorResolver errorResolver;

	@Autowired
	public OAuthErrorHandler(ErrorResolver errorResolver) {
		this.errorResolver = errorResolver;
	}

	@Override
	public ResponseEntity<OAuth2Exception> translate(Exception e) throws Exception {

		if (e instanceof OAuth2Exception) {
			ResponseEntity<OAuth2Exception> translate = super.translate(e);
			OAuth2Exception body = translate.getBody();
			body.addAdditionalInformation("message", body.getMessage());
			body.addAdditionalInformation("error_code", String.valueOf(ErrorType.ACCESS_DENIED.getCode()));
			return translate;
		} else {
			RestError restError = errorResolver.resolveError(e);
			OAuth2Exception exception = OAuth2Exception
					.create(String.valueOf(restError.getErrorRS().getErrorType().getCode()), restError.getErrorRS().getMessage());
			exception.addAdditionalInformation("message", restError.getErrorRS().getMessage());
			return new ResponseEntity<>(exception, restError.getHttpStatus());
		}

	}
}