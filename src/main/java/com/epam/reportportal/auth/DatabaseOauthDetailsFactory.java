/*
 * Copyright 2016 EPAM Systems
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
package com.epam.reportportal.auth;

import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;

import javax.inject.Provider;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.reflect.Reflection.newProxy;

/**
 * Wraps resource details with proxy to be able to update {@link OAuth2ProtectedResourceDetails}
 * on the each method call
 *
 * @author Andrei Varabyeu
 */
public class DatabaseOauthDetailsFactory {

    @Autowired
    private Provider<ServerSettingsRepository> settingsRepository;

    public OAuth2ProtectedResourceDetails getResourceDetails() {
        return newProxy(OAuth2ProtectedResourceDetails.class,
                (proxy, method, args) -> {
                    try {
                        //settingsRepository.findOne("default");
                        OAuth2ProtectedResourceDetails details = null;
                        return method.invoke(details, args);
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                });
    }

}
