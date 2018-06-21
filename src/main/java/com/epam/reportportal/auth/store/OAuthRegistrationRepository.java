package com.epam.reportportal.auth.store;

import com.epam.reportportal.auth.store.entity.OAuthRegistration;
import org.springframework.data.repository.CrudRepository;

public interface OAuthRegistrationRepository extends CrudRepository<OAuthRegistration, String> {
}