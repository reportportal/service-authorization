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
package com.epam.reportportal.auth.store.events;

import com.epam.reportportal.auth.store.entity.AuthConfigEntity;
import com.epam.reportportal.auth.util.Encryptor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent;
import org.springframework.data.mongodb.core.mapping.event.MongoMappingEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Decrypts auth config passwords if present
 *
 * @author Andrei Varabyeu
 */
@Component
public class AuthAttributesEventListener extends AbstractMongoEventListener<AuthConfigEntity> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthAttributesEventListener.class);
	private static final String MANAGER_PASSWORD_FIELD = "managerPassword";

	@Autowired
	private Encryptor encryptor;

	@Override
	public void onApplicationEvent(MongoMappingEvent<?> event) {
		super.onApplicationEvent(event);
	}

	@Override
	public void onAfterLoad(AfterLoadEvent<AuthConfigEntity> event) {
		Optional.ofNullable(event.getSource()).flatMap(dbo -> Optional.ofNullable(dbo.get("ldap"))).ifPresent(ldapDbo -> {
			DBObject ldap = ((DBObject) ldapDbo);
			Object managerPassword = ldap.get(MANAGER_PASSWORD_FIELD);
			if (null != managerPassword) {
				try {
					String decrypted = encryptor.decrypt((String) managerPassword);
					ldap.put(MANAGER_PASSWORD_FIELD, decrypted);
				} catch (Exception e) {
					LOGGER.error("Cannot decrypt password", e);
					//do nothing
				}

			}
		});
	}

}