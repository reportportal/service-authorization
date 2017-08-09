/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.auth.validation;

import com.epam.reportportal.auth.store.entity.AbstractAuthConfig;
import com.epam.reportportal.auth.store.entity.ldap.ActiveDirectoryConfig;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Applies validations if auth is enabled
 *
 * @author Andrei Varabyeu
 */
public class EnabledAuthSequenceProvider implements DefaultGroupSequenceProvider<AbstractAuthConfig> {

    @Override
    public List<Class<?>> getValidationGroups(AbstractAuthConfig authConfig) {
        if (null == authConfig) {
            return Collections.singletonList(AbstractAuthConfig.class);
        }

        List<Class<?>> sequence = new ArrayList<>();

        // Apply all validation rules from ConditionalValidation group
        // only if someField has given value
        if (isTrue(authConfig.isEnabled())) {
            sequence.add(IfEnabled.class);
        }

        // Apply all validation rules from default group
        sequence.add(authConfig.getClass());

        return sequence;
    }
}
