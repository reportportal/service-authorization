package com.epam.reportportal.auth.validation;

import com.epam.reportportal.auth.store.entity.ldap.ActiveDirectoryConfig;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

public class EnabledAuthSequenceProviderTest {

    private static Validator validator;

    @BeforeClass
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void shouldFailValidationWithNullInputValuesForTextField() {

        ActiveDirectoryConfig adConfig = new ActiveDirectoryConfig();
        adConfig.setEnabled(true);

        final Set<ConstraintViolation<ActiveDirectoryConfig>> constrants = validator
                .validate(adConfig);
//        Assert.assertThat(constrants.size(), CoreMatchers.is(6));
    }

}
