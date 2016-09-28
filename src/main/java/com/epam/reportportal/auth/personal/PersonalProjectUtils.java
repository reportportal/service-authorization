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
import com.epam.ta.reportportal.database.entity.ProjectSpecific;
import com.epam.ta.reportportal.database.entity.StatisticsCalculationStrategy;
import com.epam.ta.reportportal.database.entity.project.*;
import com.epam.ta.reportportal.database.entity.user.User;
import com.google.common.collect.ImmutableMap;

import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.ArrayList;

/**
 * Generates Personal project for provided user
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public final class PersonalProjectUtils {

	private PersonalProjectUtils() {
	}

	/**
	 * @param username Name of user
	 * @return Corresponding personal project name
	 */
	public static String personalProjectName(String username) {
		return username + "'s project";
	}

	/**
	 * Generates personal project for provided user
	 *
	 * @param user User project should be created for
	 * @return Built Project object
	 */
	public static Project generatePersonalProject(User user) {
		Project project = new Project();
		project.setName(personalProjectName(user.getLogin()));
		project.setCreationDate(Date.from(ZonedDateTime.now().toInstant()));

		Project.UserConfig userConfig = new Project.UserConfig();
		userConfig.setProjectRole(ProjectRole.PROJECT_MANAGER);
		userConfig.setProposedRole(ProjectRole.PROJECT_MANAGER);
		project.setUsers(ImmutableMap.<String, Project.UserConfig>builder().put(user.getId(), userConfig).build());

		project.setAddInfo("Personal project of " + user.getFullName());
		project.setConfiguration(defaultConfiguration());

		/* Default email configuration */
		ProjectUtils.setDefaultEmailCofiguration(project);

		return project;
	}

	/**
	 * @return Generated default project configuration
	 */
	public static Project.Configuration defaultConfiguration() {
		Project.Configuration defaultConfig = new Project.Configuration();
		defaultConfig.setEntryType(EntryType.PERSONAL);
		defaultConfig.setInterruptJobTime(InterruptionJobDelay.ONE_DAY.getValue());
		defaultConfig.setKeepLogs(KeepLogsDelay.TWO_WEEKS.getValue());
		defaultConfig.setKeepScreenshots(KeepScreenshotsDelay.ONE_WEEK.getValue());
		defaultConfig.setProjectSpecific(ProjectSpecific.DEFAULT);
		defaultConfig.setStatisticsCalculationStrategy(StatisticsCalculationStrategy.TEST_BASED);
		defaultConfig.setExternalSystem(new ArrayList<>());
		defaultConfig.setIsAutoAnalyzerEnabled(false);

		return defaultConfig;

	}
}
