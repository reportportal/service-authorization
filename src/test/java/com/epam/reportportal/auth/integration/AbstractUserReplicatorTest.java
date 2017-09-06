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
package com.epam.reportportal.auth.integration;

import com.epam.ta.reportportal.database.DataStorage;
import com.epam.ta.reportportal.database.dao.ProjectRepository;
import com.epam.ta.reportportal.database.dao.UserRepository;
import com.epam.ta.reportportal.database.entity.user.User;
import com.epam.ta.reportportal.database.entity.user.UserRole;
import com.epam.ta.reportportal.database.personal.PersonalProjectService;
import com.epam.ta.reportportal.database.search.Queryable;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Calendar;
import java.util.Date;

import static org.apache.commons.lang3.time.DateUtils.isSameDay;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andrei Varabyeu
 */
public class AbstractUserReplicatorTest {

	@Test
	public void generatePersonalProject() throws Exception {
		User user = new User();
		user.setLogin("login");
		user.setEmail("email@email.com");
		user.setIsExpired(false);
		user.setRole(UserRole.USER);
		String personalProject = replicator().generatePersonalProject(user);
		Assert.assertEquals("login_personal", personalProject);
	}

	@Test
	public void defaultMetaInfo() throws Exception {
		User.MetaInfo metaInfo = replicator().defaultMetaInfo();
		Date now = Calendar.getInstance().getTime();
		Assert.assertTrue(isSameDay(metaInfo.getLastLogin(), now));
		Assert.assertTrue(isSameDay(metaInfo.getSynchronizationDate(), now));
	}

	@Test
	public void checkEmail() throws Exception {
		UserRepository userRepoMock = mock(UserRepository.class);
		when(userRepoMock.exists(Mockito.any(Queryable.class))).thenReturn(false);

		AbstractUserReplicator replicator = new AbstractUserReplicator(userRepoMock, mock(ProjectRepository.class),
				new PersonalProjectService(mock(ProjectRepository.class)), mock(DataStorage.class));
		replicator.checkEmail("existing@email.com");
	}

	private AbstractUserReplicator replicator() {
		return new AbstractUserReplicator(mock(UserRepository.class), mock(ProjectRepository.class),
				new PersonalProjectService(mock(ProjectRepository.class)), mock(DataStorage.class));
	}

}
