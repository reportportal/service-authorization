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

import com.epam.reportportal.auth.integration.core.CreateIntegrationHandler;
import com.epam.reportportal.auth.integration.core.DeleteIntegrationHandler;
import com.epam.reportportal.auth.integration.core.GetIntegrationHandler;
import com.epam.ta.reportportal.commons.ReportPortalUser;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.integration.IntegrationRQ;
import com.epam.ta.reportportal.ws.model.integration.IntegrationResource;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@RestController
@RequestMapping(value = "/integration")
public class IntegrationEndpoint {

	private final CreateIntegrationHandler createIntegrationHandler;
	private final GetIntegrationHandler getIntegrationHandler;
	private final DeleteIntegrationHandler deleteIntegrationHandler;

	@Autowired
	public IntegrationEndpoint(CreateIntegrationHandler createIntegrationHandler, GetIntegrationHandler getIntegrationHandler,
			DeleteIntegrationHandler deleteIntegrationHandler) {
		this.createIntegrationHandler = createIntegrationHandler;
		this.getIntegrationHandler = getIntegrationHandler;
		this.deleteIntegrationHandler = deleteIntegrationHandler;
	}

	@Transactional(readOnly = true)
	@GetMapping("/global/all")
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation("Get available global integrations")
	public List<IntegrationResource> getGlobalIntegrations() {
		return getIntegrationHandler.getGlobalIntegrations();
	}

	@Transactional(readOnly = true)
	@GetMapping("/global/all/{pluginName}")
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation("Get available global integrations for plugin")
	public List<IntegrationResource> getGlobalIntegrations(@PathVariable String pluginName) {
		return getIntegrationHandler.getGlobalIntegrations(pluginName);
	}

	@Transactional
	@PostMapping(value = "/{pluginName}")
	@ResponseStatus(HttpStatus.CREATED)
	@ApiOperation("Create global Report Portal integration instance")
	public EntryCreatedRS createGlobalIntegration(@RequestBody @Valid IntegrationRQ createRequest, @PathVariable String pluginName,
			@AuthenticationPrincipal ReportPortalUser user) {
		return createIntegrationHandler.createGlobalIntegration(createRequest, pluginName, user);
	}

	@Transactional(readOnly = true)
	@GetMapping(value = "/{integrationId}")
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation("Get global Report Portal integration instance")
	public IntegrationResource getGlobalIntegration(@PathVariable Long integrationId) {
		return getIntegrationHandler.getGlobalIntegrationById(integrationId);
	}

	@Transactional
	@PutMapping(value = "/{integrationId}")
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation("Update global Report Portal integration instance")
	public OperationCompletionRS updateGlobalIntegration(@PathVariable Long integrationId, @RequestBody @Valid IntegrationRQ updateRequest) {
		return createIntegrationHandler.updateGlobalIntegration(integrationId, updateRequest);

	}

	@Transactional
	@DeleteMapping(value = "/{integrationId}")
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation("Delete integration instance")
	public OperationCompletionRS deleteGlobalIntegration(@PathVariable Long integrationId) {
		return deleteIntegrationHandler.deleteGlobalIntegration(integrationId);
	}

	@Transactional
	@DeleteMapping(value = "/all/{type}")
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation("Delete all integrations assigned to specified project")
	public OperationCompletionRS deleteAllIntegrations(@PathVariable String type) {
		return deleteIntegrationHandler.deleteGlobalIntegrationsByType(type);
	}

}
