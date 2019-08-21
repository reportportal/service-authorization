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

package com.epam.reportportal.auth.plugin;

import com.epam.reportportal.extension.auth.InitializingExtensionPoint;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.entity.integration.IntegrationTypeDetails;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.pf4j.DefaultExtensionFactory;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ReportPortalExtensionFactory extends DefaultExtensionFactory {

	private final String resourcesDir;
	private final IntegrationTypeRepository integrationTypeRepository;
	private final PluginManager pluginManager;
	private final AutowireCapableBeanFactory context;
	private final Map<String, Object> cache;

	public ReportPortalExtensionFactory(String resourcesDir, IntegrationTypeRepository integrationTypeRepository,
			PluginManager pluginManager, AutowireCapableBeanFactory context) {
		this.resourcesDir = resourcesDir;
		this.integrationTypeRepository = integrationTypeRepository;
		this.pluginManager = pluginManager;
		this.context = context;
		this.cache = Maps.newConcurrentMap();
	}

	@Override
	public Object create(Class<?> extensionClass) {
		PluginWrapper pluginWrapper = pluginManager.whichPlugin(extensionClass);
		return ofNullable(cache.get(pluginWrapper.getDescriptor().getPluginId())).orElseGet(() -> {
			try {
				IntegrationType integrationType = integrationTypeRepository.findByName(pluginWrapper.getPluginId())
						.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, pluginWrapper.getPluginId()));
				Map<String, Object> initParams = ofNullable(integrationType.getDetails()).map(IntegrationTypeDetails::getDetails)
						.orElseGet(() -> {
							HashMap<String, Object> details = Maps.newHashMap();
							IntegrationTypeDetails integrationTypeDetails = new IntegrationTypeDetails();
							integrationTypeDetails.setDetails(details);
							integrationType.setDetails(integrationTypeDetails);
							return details;
						});
				initParams.put(IntegrationDetailsProperties.RESOURCES_DIRECTORY.getAttribute(),
						Paths.get(resourcesDir, pluginWrapper.getPluginId()).toString()
				);
				Object withoutSpring = extensionClass.getDeclaredConstructor(Map.class).newInstance(initParams);
				if (InitializingExtensionPoint.class.isAssignableFrom(withoutSpring.getClass())) {
					InitializingExtensionPoint initializingExtensionPoint = (InitializingExtensionPoint) withoutSpring;
					ofNullable(initializingExtensionPoint.getBeansToInitialize()).ifPresent(beansMapping -> {
						Set<String> beanNames = Sets.newLinkedHashSetWithExpectedSize(beansMapping.size());
						for (Map.Entry<String, Class<?>> entry : beansMapping.entrySet()) {
							try {
								Object bean = context.createBean(entry.getValue());
								((AbstractAutowireCapableBeanFactory) context).registerSingleton(entry.getKey(), bean);
								beanNames.add(entry.getKey());
							} catch (Exception ex) {
								beanNames.forEach(beanName -> {
									if (context.containsBean(beanName)) {
										((AbstractAutowireCapableBeanFactory) context).destroySingleton(beanName);
									}
								});
								beanNames.clear();
								throw new ReportPortalException(ErrorType.PLUGIN_UPLOAD_ERROR,
										"Error during beans initialization: " + ex.getMessage()
								);
							}

						}
					});

				}
				context.autowireBean(withoutSpring);
				context.initializeBean(withoutSpring, pluginWrapper.getDescriptor().getPluginId());
				((AbstractAutowireCapableBeanFactory) context).registerSingleton(pluginWrapper.getDescriptor().getPluginId(),
						withoutSpring
				);
				cache.put(pluginWrapper.getDescriptor().getPluginId(), withoutSpring);
				return withoutSpring;
			} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
				throw new ReportPortalException(ErrorType.PLUGIN_UPLOAD_ERROR, "Unable to create plugin instance: " + e.getMessage());
			}

		});

	}
}
