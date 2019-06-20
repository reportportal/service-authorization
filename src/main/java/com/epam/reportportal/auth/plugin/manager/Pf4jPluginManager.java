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

package com.epam.reportportal.auth.plugin.manager;

import com.epam.reportportal.auth.plugin.entity.Plugin;
import com.epam.reportportal.extension.common.ExtensionPoint;
import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.AbstractIdleService;
import org.pf4j.*;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.lang.Nullable;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class Pf4jPluginManager extends AbstractIdleService implements Pf4jPluginBox {

	private static final long MAXIMUM_UPLOADED_PLUGINS = 50;
	private static final long PLUGIN_LIVE_TIME = 2;

	private final AutowireCapableBeanFactory context;
	private final org.pf4j.PluginManager pluginManager;

	private final Cache<String, Path> uploadingPlugins;

	public Pf4jPluginManager(String pluginsPath, AutowireCapableBeanFactory context,
			Collection<PluginDescriptorFinder> pluginDescriptorFinders) {
		this.context = context;
		uploadingPlugins = CacheBuilder.newBuilder()
				.maximumSize(MAXIMUM_UPLOADED_PLUGINS)
				.expireAfterWrite(PLUGIN_LIVE_TIME, TimeUnit.MINUTES)
				.build();
		pluginManager = new DefaultPluginManager(FileSystems.getDefault().getPath(pluginsPath)) {
			@Override
			protected CompoundPluginDescriptorFinder createPluginDescriptorFinder() {

				CompoundPluginDescriptorFinder compoundPluginDescriptorFinder = new CompoundPluginDescriptorFinder();

				pluginDescriptorFinders.forEach(compoundPluginDescriptorFinder::add);

				return
						// Demo is using the Manifest file
						// PropertiesPluginDescriptorFinder is commented out just to avoid error log
						//.add(new PropertiesPluginDescriptorFinder())
						compoundPluginDescriptorFinder;
			}

			@Override
			protected ExtensionFactory createExtensionFactory() {
				return new DefaultExtensionFactory() {
					@Override
					public Object create(Class<?> extensionClass) {
						Object obj = super.create(extensionClass);
						if (null == obj) {
							return null;
						}
						Pf4jPluginManager.this.context.autowireBean(obj);
						return obj;
					}
				};
			}
		};
	}

	@Override
	public List<com.epam.reportportal.auth.plugin.entity.Plugin> getPlugins() {
		return this.pluginManager.getPlugins()
				.stream()
				.flatMap(plugin -> pluginManager.getExtensionClasses(plugin.getPluginId())
						.stream()
						.map(ExtensionPoint::findByExtension)
						.filter(Optional::isPresent)
						.map(it -> new com.epam.reportportal.auth.plugin.entity.Plugin(plugin.getPluginId(), it.get())))
				.collect(Collectors.toList());
	}

	@Override
	public Optional<Plugin> getPlugin(String type) {
		return getPlugins().stream().filter(p -> p.getType().name().equalsIgnoreCase(type)).findAny();
	}

	@Override
	public <T> Optional<T> getInstance(Class<T> extension) {
		return pluginManager.getExtensions(extension).stream().findFirst();
	}

	@Override
	public PluginState startUpPlugin(String pluginId) {

		PluginWrapper pluginWrapper = ofNullable(pluginManager.getPlugin(pluginId)).orElseThrow(() -> new ReportPortalException(ErrorType.UNABLE_INTERACT_WITH_INTEGRATION,
				"Plugin not found: " + pluginId
		));

		return pluginManager.startPlugin(pluginWrapper.getPluginId());
	}

	@Override
	@Nullable
	public String loadPlugin(Path path) {
		return pluginManager.loadPlugin(path);
	}

	@Override
	public boolean unloadPlugin(String pluginId) {
		return pluginManager.unloadPlugin(pluginId);
	}

	@Override
	public Optional<PluginWrapper> getPluginById(String id) {
		return ofNullable(pluginManager.getPlugin(id));
	}

	@Override
	public void addUploadingPlugin(String fileName, Path path) {

		uploadingPlugins.put(fileName, path);
	}

	@Override
	public void removeUploadingPlugin(String fileName) {

		uploadingPlugins.invalidate(fileName);
	}

	@Override
	public boolean isPluginStillBeingUploaded(String fileName) {

		return uploadingPlugins.asMap().containsKey(fileName);
	}

	@Override
	public <T> Optional<T> getInstance(String name, Class<T> extension) {
		return pluginManager.getExtensions(extension, name).stream().findFirst();
	}

	@Override
	protected void startUp() {
		// start and load all plugins of application
		pluginManager.loadPlugins();
		pluginManager.startPlugins();

	}

	@Override
	protected void shutDown() {
		// stop and unload all plugins
		pluginManager.stopPlugins();
		pluginManager.getPlugins().forEach(p -> pluginManager.unloadPlugin(p.getPluginId()));
	}
}
