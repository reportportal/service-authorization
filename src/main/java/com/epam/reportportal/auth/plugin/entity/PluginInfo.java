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

package com.epam.reportportal.auth.plugin.entity;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class PluginInfo implements Serializable {

	private String id;

	private String version;

	private String group;

	private String service;

	private String fileId;

	private String fileName;

	private boolean isEnabled;

	public PluginInfo() {
	}

	public PluginInfo(String id, String version, String group, String service) {
		this.id = id;
		this.version = version;
		this.group = group;
		this.service = service;
	}

	public PluginInfo(String id, String version, String group, String service, String fileId, String fileName, boolean isEnabled) {
		this.id = id;
		this.version = version;
		this.group = group;
		this.service = service;
		this.fileId = fileId;
		this.fileName = fileName;
		this.isEnabled = isEnabled;
	}

	@Nullable
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Nullable
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
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

	@Nullable
	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	@Nullable
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}

}
