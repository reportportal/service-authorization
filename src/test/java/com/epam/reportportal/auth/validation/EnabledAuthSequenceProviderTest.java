package com.epam.reportportal.auth.validation;

import com.epam.reportportal.auth.store.entity.ldap.ActiveDirectoryConfig;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

public class EnabledAuthSequenceProviderTest {

    @Test
    public void shouldFailValidationWithNullInputValuesForTextField() {
        ValidatorFactory config = Validation.buildDefaultValidatorFactory();
        Validator validator = config.getValidator();

        ActiveDirectoryConfig adConfig = new ActiveDirectoryConfig();
        adConfig.setEnabled(false);

        final Set<ConstraintViolation<ActiveDirectoryConfig>> constrants = validator
                .validate(adConfig, IfEnabled.class);
        Assert.assertThat(constrants.size(), CoreMatchers.is(6));
    }

}
