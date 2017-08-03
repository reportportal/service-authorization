package com.epam.reportportal.auth.integration;

import com.epam.ta.reportportal.database.dao.ProjectRepository;
import com.epam.ta.reportportal.database.dao.UserRepository;
import com.epam.ta.reportportal.database.personal.PersonalProjectService;
import org.junit.Test;

public class AbstractUserReplicatorTest {

	@Test
	public void generatePersonalProject() throws Exception {

	}

	@Test
	public void defaultMetaInfo() throws Exception {
	}

	@Test
	public void checkEmail() throws Exception {
	}

	private AbstractUserReplicator replicator() {
		return new AbstractUserReplicator(Mockito.mock(UserRepository.class), mock(ProjectRepository.class), mock(PersonalProjectService.class));
	}

}