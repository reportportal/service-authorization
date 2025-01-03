package com.epam.ta.reportportal.dao;

import com.epam.ta.reportportal.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryCustomImplTest extends BaseTest {

  @Autowired
  UserRepository userRepository;

  @Test
  void findUserDetails() {
    var user = userRepository.findByLogin("superadmin")
        .orElseThrow();
    Assertions.assertEquals("superadmin", user.getLogin());
  }

  @Test
  void findUserDetailsNotFound() {
    var user = userRepository.findByLogin("notfound");
    Assertions.assertTrue(user.isEmpty());
  }

}
