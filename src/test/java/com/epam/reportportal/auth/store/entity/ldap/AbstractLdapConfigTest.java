package com.epam.reportportal.auth.store.entity.ldap;

import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;

public class AbstractLdapConfigTest {

	private Validator validator;

	@Before
	public void init() {
		ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
		this.validator = vf.getValidator();
	}

	@Test
	public void checkValidation() {
		LdapConfig cfg = new LdapConfig();
		cfg.setEnabled(true);
		cfg.setBaseDn("someDN");
		cfg.setUrl("ldaps://localhost");
		Set<ConstraintViolation<LdapConfig>> violations = validator.validate(cfg);
		assertTrue(violations.isEmpty());
	}
}
