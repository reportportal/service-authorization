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
import com.epam.reportportal.auth.store.AuthConfigRepository;
import com.epam.reportportal.auth.store.entity.AbstractAuthConfig;
import com.epam.reportportal.auth.store.entity.ldap.ActiveDirectoryConfig;
import com.epam.reportportal.auth.store.entity.ldap.LdapConfig;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.beans.PropertyEditorSupport;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Controller
@RequestMapping("/settings/auth")
@Api(description = "Main Auth Configuration Endpoint")
public class AuthConfigurationEndpoint {

    private final AuthConfigRepository repository;

    @Autowired
    public AuthConfigurationEndpoint(AuthConfigRepository repository) {
        this.repository = repository;
        this.repository.createDefaultProfileIfAbsent();
    }

    /**
     * Updates LDAP auth settings
     *
     * @param ldapConfig LDAP configuration
     * @return Successful message or an error
     */
    @RequestMapping(value = "/ldap", method = { POST, PUT })
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Updates LDAP auth settings")
    public LdapConfig updateLdapSettings(@RequestBody @Validated LdapConfig ldapConfig) {
        repository.updateLdap(ldapConfig);
        return repository.findDefault().getLdap();
    }

    /**
     * Updates LDAP auth settings
     *
     * @param adConfig Active Directory configuration
     * @return Successful message or an error
     */
    @RequestMapping(value = "/ad", method = { POST, PUT })
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Updates LDAP auth settings")
    public ActiveDirectoryConfig updateADSettings(@RequestBody @Validated ActiveDirectoryConfig adConfig) {
        repository.updateActiveDirectory(adConfig);
        return repository.findDefault().getActiveDirectory();
    }

    /**
     * Updates LDAP auth settings
     *
     * @param authType Type of Auth
     * @return Successful message or an error
     */
    @RequestMapping(value = "/{authType}", method = { GET })
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Retrieves auth settings")
    public AbstractAuthConfig getSettings(@PathVariable AuthIntegrationType authType) {
        return authType.get(repository.findDefault())
                .orElseThrow(() -> new ReportPortalException(ErrorType.OAUTH_INTEGRATION_NOT_FOUND, authType.getId()));
    }

    /**
     * Deletes LDAP auth settings
     *
     * @param authType Type of Auth
     * @return Successful message or an error
     */
    @RequestMapping(value = "/{authType}", method = { DELETE })
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Retrieves auth settings")
    public OperationCompletionRS deleteSettings(@PathVariable AuthIntegrationType authType) {
        repository.deleteSettings(authType);
        return new OperationCompletionRS(String.format("Auth config %s successfully deleted",authType));
    }

    @InitBinder
    public void initBinder(final WebDataBinder webdataBinder) {
        webdataBinder.registerCustomEditor(AuthIntegrationType.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                setValue(AuthIntegrationType.fromId(text).orElse(null));
            }
        });
    }
}
