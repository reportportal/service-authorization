/*
 * Copyright 2017 EPAM Systems
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
package com.epam.reportportal.auth.endpoint;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.handler.CreateAuthIntegrationHandler;
import com.epam.reportportal.auth.integration.handler.DeleteAuthIntegrationHandler;
import com.epam.reportportal.auth.integration.handler.GetAuthIntegrationHandler;
import com.epam.reportportal.auth.util.Encryptor;
import com.epam.ta.reportportal.entity.integration.Integration;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.integration.auth.ActiveDirectoryResource;
import com.epam.ta.reportportal.ws.model.integration.auth.LdapResource;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateActiveDirectoryRQ;
import com.epam.ta.reportportal.ws.model.integration.auth.UpdateLdapRQ;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.beans.PropertyEditorSupport;

import static java.util.Optional.ofNullable;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/settings/auth")
@Api(description = "Main Auth Configuration Endpoint")
public class AuthConfigurationEndpoint {

	private final CreateAuthIntegrationHandler createAuthIntegrationHandler;

	private final DeleteAuthIntegrationHandler deleteAuthIntegrationHandler;

	private final GetAuthIntegrationHandler getAuthIntegrationHandler;

	private final Encryptor encryptor;

	@Autowired
	public AuthConfigurationEndpoint(CreateAuthIntegrationHandler createAuthIntegrationHandler,
			DeleteAuthIntegrationHandler deleteAuthIntegrationHandler, GetAuthIntegrationHandler getAuthIntegrationHandler,
			Encryptor encryptor) {
		this.createAuthIntegrationHandler = createAuthIntegrationHandler;
		this.deleteAuthIntegrationHandler = deleteAuthIntegrationHandler;
		this.getAuthIntegrationHandler = getAuthIntegrationHandler;
		this.encryptor = encryptor;
	}

	/**
	 * Updates LDAP auth settings
	 *
	 * @param updateLdapRQ LDAP configuration
	 * @return Successful message or an error
	 */
	@Transactional
	@RequestMapping(value = "/ldap", method = { POST, PUT })
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Updates LDAP auth settings")
	public LdapResource updateLdapSettings(@RequestBody @Valid UpdateLdapRQ updateLdapRQ) {

		encryptPasswords(updateLdapRQ);
		return createAuthIntegrationHandler.updateLdapSettings(updateLdapRQ);
	}

	/**
	 * Updates LDAP auth settings
	 *
	 * @param updateActiveDirectoryRQ Active Directory configuration
	 * @return Successful message or an error
	 */
	@Transactional
	@RequestMapping(value = "/ad", method = { POST, PUT })
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Updates LDAP auth settings")
	public ActiveDirectoryResource updateADSettings(@RequestBody @Validated UpdateActiveDirectoryRQ updateActiveDirectoryRQ) {

		return createAuthIntegrationHandler.updateActiveDirectorySettings(updateActiveDirectoryRQ);
	}

	/**
	 * Updates LDAP auth settings
	 *
	 * @param authType Type of Auth
	 * @return Successful message or an error
	 */
	@Transactional(readOnly = true)
	@GetMapping(value = "/{authType}")
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Retrieves auth settings")
	public Integration getSettings(@PathVariable AuthIntegrationType authType) {

		return getAuthIntegrationHandler.getIntegrationByType(authType);
	}

	/**
	 * Deletes LDAP auth settings
	 *
	 * @param integrationId Type of Auth
	 * @return Successful message or an error
	 */
	@Transactional
	@DeleteMapping(value = "/{integrationId}")
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Retrieves auth settings")
	public OperationCompletionRS deleteSettings(@PathVariable Long integrationId) {

		return deleteAuthIntegrationHandler.deleteAuthIntegrationById(integrationId);
	}

	@InitBinder
	public void initBinder(final WebDataBinder webdataBinder) {
		webdataBinder.registerCustomEditor(AuthIntegrationType.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				setValue(AuthIntegrationType.fromId(text)
						.orElseThrow(() -> new ReportPortalException(ErrorType.INCORRECT_AUTHENTICATION_TYPE, text)));
			}
		});
	}

	private void encryptPasswords(UpdateLdapRQ updateLdapRQ) {
		ofNullable(updateLdapRQ).flatMap(ldap -> ofNullable(ldap.getManagerPassword()))
				.ifPresent(pwd -> updateLdapRQ.setManagerPassword(encryptor.encrypt(updateLdapRQ.getManagerPassword())));
	}
}