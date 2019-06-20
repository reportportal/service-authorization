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

import org.pf4j.DefaultPluginDescriptor;
import org.pf4j.PluginDescriptor;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class RpPluginDescriptor extends DefaultPluginDescriptor {

	private String group;

	private String service;

	public RpPluginDescriptor() {

	}

	public RpPluginDescriptor(PluginDescriptor pluginDescriptor, String group, String service) {
		super(pluginDescriptor.getPluginId(),
				pluginDescriptor.getPluginDescription(),
				pluginDescriptor.getPluginClass(),
				pluginDescriptor.getVersion(),
				pluginDescriptor.getRequires(),
				pluginDescriptor.getProvider(),
				pluginDescriptor.getLicense()
		);
		this.group = group;
		this.service = service;
	}

	public RpPluginDescriptor(String pluginId, String pluginDescription, String pluginClass, String version, String requires,
			String provider, String license, String group, String service) {
		super(pluginId, pluginDescription, pluginClass, version, requires, provider, license);
		this.group = group;
		this.service = service;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}
}
