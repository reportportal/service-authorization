package com.epam.reportportal.auth.store;

import com.epam.reportportal.auth.store.entity.OAuthRegistration;
import com.epam.ta.reportportal.dao.ReportPortalRepository;

public interface OAuthRegistrationRepository extends ReportPortalRepository<OAuthRegistration, String>, OAuthRegistrationRepositoryCustom {
}