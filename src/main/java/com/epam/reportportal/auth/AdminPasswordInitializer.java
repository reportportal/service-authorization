package com.epam.reportportal.auth;

import com.epam.reportportal.auth.exception.EnvironmentVariablesNotProvidedException;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.user.User;
import javax.persistence.EntityNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminPasswordInitializer implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminPasswordInitializer.class);
  private static final String SUPER_ADMIN_LOGIN = "superadmin";
  private static final String ERROR_MSG = "Password not set in environment variable";

  private final UserRepository userRepository;

  @Value("${rp.admin.password:}")
  private String adminPassword;

  public AdminPasswordInitializer(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional
  public void run(String... args) {
    checkPasswordEnvVariable();

    User user = userRepository.findByLogin(SUPER_ADMIN_LOGIN)
        .orElseThrow(() -> new EntityNotFoundException(SUPER_ADMIN_LOGIN + " not found"));

    boolean isMatches = passwordEncoder().matches(adminPassword, user.getPassword());
    if (!isMatches) {
      updatePasswordForDefaultAdmin(user);
    }
  }

  private void updatePasswordForDefaultAdmin(User defaultAdmin) {
    defaultAdmin.setPassword(passwordEncoder().encode(adminPassword));
    userRepository.save(defaultAdmin);
  }

  private void checkPasswordEnvVariable() {
    if (StringUtils.isBlank(adminPassword)) {
      LOGGER.error(ERROR_MSG);
      throw new EnvironmentVariablesNotProvidedException(ERROR_MSG);
    }
  }

  private PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
