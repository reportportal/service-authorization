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

import com.epam.reportportal.auth.plugin.plugin.PluginLoader;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import org.pf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Paths;

@Configuration
public class PluginConfiguration {

	@Autowired
	private AutowireCapableBeanFactory context;

	@Autowired
	private PluginLoader pluginLoader;

	@Autowired
	private IntegrationTypeRepository integrationTypeRepository;

	@Value("${rp.plugins.resolved.path}")
	private String pluginsPath;

	@Value("${rp.plugins.temp.path}")
	private String pluginsTempPath;

	@Value("${rp.plugins.resolved.resources}")
	private String resourcesDir;

	@Bean
	public Pf4jPluginBox pf4jPluginBox() throws IOException {
		Pf4jPluginManager manager = new Pf4jPluginManager(pluginsPath,
				pluginsTempPath,
				resourcesDir,
				pluginLoader,
				integrationTypeRepository,
				pluginManager(),
				context
		);
		manager.startAsync();
		return manager;
	}

	@Bean
	public PluginManager pluginManager() {

		return new DefaultPluginManager(Paths.get(pluginsPath)) {
			@Override
			protected PluginDescriptorFinder createPluginDescriptorFinder() {
				return pluginDescriptorFinder();
			}

			@Override
			protected ExtensionFactory createExtensionFactory() {
				return new ReportPortalExtensionFactory(resourcesDir, integrationTypeRepository, this, context);
			}
		};
	}

	@Bean
	public PluginDescriptorFinder pluginDescriptorFinder() {
		return new ManifestPluginDescriptorFinder();
	}

}
