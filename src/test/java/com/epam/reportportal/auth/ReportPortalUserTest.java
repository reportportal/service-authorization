/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.auth;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertThat;

/**
 * @author Andrei Varabyeu
 */
public class ReportPortalUserTest {

	@Test
	public void testSpringUserFilled() {
		ReportPortalUser u = new ReportPortalUser("name", "pass", Collections.emptyList(), null);
		assertThat(u.getUsername(), CoreMatchers.is("name"));
		assertThat(u.getPassword(), CoreMatchers.is("pass"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullPassNotAllowed() {
		new ReportPortalUser("name", null, Collections.emptyList(), null);
	}
}