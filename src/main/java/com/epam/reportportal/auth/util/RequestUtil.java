/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.auth.util;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link org.apache.catalina.util.RequestUtil#getRequestURL} analogue
 * with replaced {@link HttpServletRequest#getRequestURI()} to {@link HttpServletRequest#getContextPath()} method invocation
 *
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class RequestUtil {

	private RequestUtil() {
		//static only
	}

	public static String getRequestBasePath(HttpServletRequest request) {

		StringBuilder url = new StringBuilder();
		String scheme = request.getScheme();
		int port = request.getServerPort();
		if (port < 0) {
			// Work around java.net.URL bug
			port = 80;
		}

		url.append(scheme);
		url.append("://");
		url.append(request.getServerName());
		if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
			url.append(':');
			url.append(port);
		}

		url.append(request.getContextPath());

		return url.toString();
	}

}
