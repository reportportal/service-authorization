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
package com.epam.reportportal.auth.integration.saml;

import java.io.Serializable;

/**
 * Represents information about subject of authentication stored in SAML response
 *
 * @author Yevgeniy Svalukhin
 */
public class Subject implements Serializable {

	private static final long serialVersionUID = 2390092323L;

	private SamlPrincipal samlPrincipal;

	public SamlPrincipal getSamlPrincipal() {
		return samlPrincipal;
	}

	public Subject setSamlPrincipal(SamlPrincipal samlPrincipal) {
		this.samlPrincipal = samlPrincipal;
		return this;
	}
}
