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

import com.epam.reportportal.extension.auth.data.BeanData;
import com.epam.reportportal.extension.auth.data.BeanProviderData;
import com.epam.reportportal.extension.auth.provider.BeanProvider;
import com.epam.reportportal.extension.common.InitializingExtensionPoint;
import com.epam.reportportal.extension.common.IntegrationTypeProperties;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import com.epam.ta.reportportal.entity.integration.IntegrationType;
import com.epam.ta.reportportal.entity.integration.IntegrationTypeDetails;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.pf4j.DefaultExtensionFactory;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ReportPortalExtensionFactory extends DefaultExtensionFactory {

	private final String resourcesDir;
	private final IntegrationTypeRepository integrationTypeRepository;
	private final PluginManager pluginManager;
	private final AbstractAutowireCapableBeanFactory beanFactory;

	public ReportPortalExtensionFactory(String resourcesDir, IntegrationTypeRepository integrationTypeRepository,
			PluginManager pluginManager, AutowireCapableBeanFactory context) {
		this.resourcesDir = resourcesDir;
		this.integrationTypeRepository = integrationTypeRepository;
		this.pluginManager = pluginManager;
		this.beanFactory = (AbstractAutowireCapableBeanFactory) context;
	}

	@Override
	public Object create(Class<?> extensionClass) {
		PluginWrapper pluginWrapper = pluginManager.whichPlugin(extensionClass);
		if (beanFactory.containsSingleton(pluginWrapper.getPluginId())) {
			return beanFactory.getSingleton(pluginWrapper.getPluginId());
		} else {
			return createExtension(extensionClass, pluginWrapper);
		}
	}

	private Object createExtension(Class<?> extensionClass, PluginWrapper pluginWrapper) {
		try {
			IntegrationType integrationType = integrationTypeRepository.findByName(pluginWrapper.getPluginId())
					.orElseThrow(() -> new ReportPortalException(ErrorType.INTEGRATION_NOT_FOUND, pluginWrapper.getPluginId()));
			Map<String, Object> initParams = getInitParams(integrationType);
			Object plugin = extensionClass.getDeclaredConstructor(Map.class).newInstance(initParams);
			if (InitializingExtensionPoint.class.isAssignableFrom(plugin.getClass())) {
				ofNullable(((InitializingExtensionPoint) plugin).getBeanProviders()).ifPresent(beanProvidersData -> {
					Map<String, List<String>> providedBeansMapping = Maps.newLinkedHashMapWithExpectedSize(beanProvidersData.size());
					for (BeanProviderData beanProviderData : beanProvidersData) {
						Pair<String, List<String>> initialized = initBeanProvider(beanProviderData);
						providedBeansMapping.put(initialized.getKey(), initialized.getValue());
					}
					IntegrationTypeProperties.DEPENDENCIES.setValue(integrationType.getDetails(), providedBeansMapping);
				});

			}
			initPlugin(plugin, pluginWrapper);
			return plugin;
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			throw new ReportPortalException(ErrorType.PLUGIN_UPLOAD_ERROR, "Unable to create plugin instance: " + e.getMessage());
		}
	}

	private Map<String, Object> getInitParams(IntegrationType integrationType) {
		Map<String, Object> initParams = ofNullable(integrationType.getDetails()).map(IntegrationTypeDetails::getDetails).orElseGet(() -> {
			HashMap<java.lang.String, java.lang.Object> details = Maps.newHashMap();
			IntegrationTypeDetails integrationTypeDetails = new IntegrationTypeDetails();
			integrationTypeDetails.setDetails(details);
			integrationType.setDetails(integrationTypeDetails);
			return details;
		});
		initParams.put(IntegrationTypeProperties.RESOURCES_DIRECTORY.getAttribute(),
				Paths.get(resourcesDir, integrationType.getName()).toString()
		);
		return initParams;
	}

	private Pair<String, List<String>> initBeanProvider(BeanProviderData beanProviderData) {
		List<BeanData> beanDataList = Collections.emptyList();
		try {
			BeanProvider beanProvider = beanFactory.createBean(beanProviderData.getBeanProviderClass());
			beanFactory.registerSingleton(beanProviderData.getName(), beanProvider);
			beanDataList = beanProvider.getBeansToInitialize();
			List<String> beanNames = beanDataList.stream()
					.peek(beanData -> beanFactory.registerSingleton(beanData.getName(), beanData.getBeanObject()))
					.map(BeanData::getName)
					.collect(Collectors.toList());
			return Pair.of(beanProviderData.getName(), beanNames);
		} catch (Exception ex) {
			if (CollectionUtils.isNotEmpty(beanDataList)) {
				beanDataList.stream().map(BeanData::getName).forEach(this::destroySingleton);
			}
			destroySingleton(beanProviderData.getName());
			throw new ReportPortalException(ErrorType.PLUGIN_UPLOAD_ERROR, "Error during beans initialization: " + ex.getMessage());
		}
	}

	private void destroySingleton(String beanName) {
		if (beanFactory.containsSingleton(beanName)) {
			beanFactory.destroySingleton(beanName);
		}
	}

	private void initPlugin(Object plugin, PluginWrapper pluginWrapper) {
		beanFactory.autowireBean(plugin);
		beanFactory.initializeBean(plugin, pluginWrapper.getDescriptor().getPluginId());
		beanFactory.registerSingleton(pluginWrapper.getDescriptor().getPluginId(), plugin);
		beanFactory.registerDisposableBean(pluginWrapper.getDescriptor().getPluginId(), (DisposableBean) plugin);
	}
}
