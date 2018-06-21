package com.epam.reportportal.auth.store;

import com.epam.reportportal.auth.store.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {

	Optional<User> findByLogin(String login);
}