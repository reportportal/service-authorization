package com.epam.reportportal.auth.store.entity.ldap;

import com.google.common.base.Joiner;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

public class AbstractLdapConfigTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLdapConfigTest.class);

	@Test
	public void checkValidation() {
		ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
		Validator validator = vf.getValidator();

		LdapConfig cfg = new LdapConfig();
		cfg.setEnabled(true);
		cfg.setBaseDn("someDN");
		cfg.setUrl("ldaps://localhost");
		Set<ConstraintViolation<LdapConfig>> violations = validator.validate(cfg);
		if (!violations.isEmpty()) {
			LOGGER.error("Violations found: " + Joiner.on("\n").join(violations));
		}
		Assert.assertThat(violations, Matchers.empty());
	}
}
