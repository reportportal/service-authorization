/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.auth.store.events;

/**
 * Decrypts auth config passwords if present
 *
 * @author Andrei Varabyeu
 */
//@Component
//public class AuthAttributesEventListener extends AbstractMongoEventListener<AuthConfigEntity> {
public class AuthAttributesEventListener {

//	private static final Logger LOGGER = LoggerFactory.getLogger(AuthAttributesEventListener.class);
//	private static final String MANAGER_PASSWORD_FIELD = "managerPassword";
//
//	@Autowired
//	private Encryptor encryptor;
//
//	@Override
//	public void onApplicationEvent(MongoMappingEvent<?> event) {
//		super.onApplicationEvent(event);
//	}
//
//	@Override
//	public void onAfterLoad(AfterLoadEvent<AuthConfigEntity> event) {
//		Optional.ofNullable(event.getSource()).flatMap(dbo -> Optional.ofNullable(dbo.get("ldap"))).ifPresent(ldapDbo -> {
//			DBObject ldap = ((DBObject) ldapDbo);
//			Object managerPassword = ldap.get(MANAGER_PASSWORD_FIELD);
//			if (null != managerPassword) {
//				try {
//					String decrypted = encryptor.decrypt((String) managerPassword);
//					ldap.put(MANAGER_PASSWORD_FIELD, decrypted);
//				} catch (Exception e) {
//					LOGGER.error("Cannot decrypt password", e);
//					//do nothing
//				}
//
//			}
//		});
//	}

}