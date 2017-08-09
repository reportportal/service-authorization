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
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies validations if auth is enabled
 *
 * @author Andrei Varabyeu
 */
public class EnabledAuthSequenceProvider implements DefaultGroupSequenceProvider<AbstractAuthConfig> {

    @Override
    public List<Class<?>> getValidationGroups(AbstractAuthConfig authConfig) {
        List<Class<?>> defaultGroupSequence = new ArrayList<>();
        if (null == authConfig) {
            defaultGroupSequence.add(AbstractAuthConfig.class);
        } else {
            defaultGroupSequence.add(authConfig.getClass());
            defaultGroupSequence.add(AbstractAuthConfig.class);
        }


        if (authConfig != null && authConfig.isEnabled()) {
            defaultGroupSequence.add(IfEnabled.class);
        }

        return defaultGroupSequence;
    }
}
