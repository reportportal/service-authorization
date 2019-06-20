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

package com.epam.reportportal.auth.plugin.core.util;

import com.epam.ta.reportportal.exception.ReportPortalException;
import com.epam.ta.reportportal.ws.model.ErrorType;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginException;

import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class RpManifestPluginDescriptorFinder extends ManifestPluginDescriptorFinder {

	@Override
	public RpPluginDescriptor find(Path pluginPath) throws PluginException {
		return createPluginDescriptor(readManifest(pluginPath));
	}

	@Override
	protected RpPluginDescriptor createPluginDescriptor(Manifest manifest) {
		PluginDescriptor pluginDescriptor = super.createPluginDescriptor(manifest);
		Attributes attributes = manifest.getMainAttributes();
		String group = ofNullable(attributes.getValue("Plugin-group")).orElseThrow(() -> new ReportPortalException(ErrorType.PLUGIN_UPLOAD_ERROR,
				"Plugin group attribute not found"
		));
		String service = ofNullable(attributes.getValue("Plugin-service")).orElseThrow(() -> new ReportPortalException(ErrorType.PLUGIN_UPLOAD_ERROR,
				"Plugin service attribute not found"
		));
		return new RpPluginDescriptor(pluginDescriptor, group, service);
	}

	@Override
	protected RpPluginDescriptor createPluginDescriptorInstance() {
		return new RpPluginDescriptor();
	}
}
