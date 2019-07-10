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
package com.epam.reportportal.auth.endpoint;

import com.epam.reportportal.auth.integration.AuthIntegrationType;
import com.epam.reportportal.auth.integration.handler.CreateAuthIntegrationHandler;
import com.epam.reportportal.auth.integration.handler.DeleteAuthIntegrationHandler;
import com.epam.reportportal.auth.integration.handler.GetAuthIntegrationHandler;
import com.epam.reportportal.auth.util.Encryptor;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.integration.auth.*;
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
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

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
	public AbstractLdapResource getSettings(@PathVariable AuthIntegrationType authType) {

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