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
package com.epam.reportportal.auth.personal;

import com.epam.ta.reportportal.database.entity.Project;
import com.epam.ta.reportportal.database.entity.ProjectRole;
import com.epam.ta.reportportal.database.entity.project.EntryType;
import com.epam.ta.reportportal.database.entity.user.User;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link PersonalProjectUtils}
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class PersonalProjectUtilsTest {

	@Test
	public void personalProjectName() throws Exception {
		Assert.assertThat("Generated personal space name is incorrect", PersonalProjectUtils.personalProjectName("John"),
				Matchers.is("John's project"));
	}

	@Test
	public void generatePersonalProject() throws Exception {
		User user = new User();
		String login = "johnny";
		user.setLogin(login);
		user.setFullName("John");

		Project project = PersonalProjectUtils.generatePersonalProject(user);
		Assert.assertThat("Project doesn't have user", project.getUsers(), Matchers.hasKey(login));
		Assert.assertThat("Incorrect role", project.getUsers().get(login).getProjectRole(), Matchers.is(ProjectRole.PROJECT_MANAGER));
		Assert.assertThat("Incorrect role", project.getUsers().get(login).getProposedRole(), Matchers.is(ProjectRole.PROJECT_MANAGER));

		Assert.assertThat("Incorrect date", project.getCreationDate(), Matchers.notNullValue());
		Assert.assertThat("Incorrect configuration", project.getConfiguration(), Matchers.notNullValue());
		Assert.assertThat("Incorrect additional info", project.getAddInfo(), Matchers.notNullValue());

	}

	@Test
	public void defaultConfiguration() throws Exception {
		Project.Configuration configuration = PersonalProjectUtils.defaultConfiguration();

		Assert.assertThat("Incorrect project type", configuration.getEntryType(), Matchers.is(EntryType.PERSONAL));
		Assert.assertThat("Incorrect keep screenshots config", configuration.getKeepScreenshots(), Matchers.notNullValue());
		Assert.assertThat("Incorrect auto analysis config", configuration.getIsAutoAnalyzerEnabled(), Matchers.is(false));
		Assert.assertThat("Incorrect interrupt config", configuration.getInterruptJobTime(), Matchers.notNullValue());
		Assert.assertThat("Incorrect keep logs config", configuration.getKeepLogs(), Matchers.notNullValue());

	}

}