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
package com.epam.reportportal.auth.store;

import com.epam.reportportal.auth.store.entity.AuthConfig;
import com.epam.reportportal.auth.store.entity.ldap.ActiveDirectoryConfig;
import com.epam.reportportal.auth.store.entity.ldap.LdapConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

/**
 * @author Andrei Varabyeu
 */
public interface AuthConfigRepository extends MongoRepository<AuthConfig, String> {

	@Query(value = "{ 'id' : 'ldap' , 'enabled' : ?0 }")
	Optional<LdapConfig> findLdap(boolean enabled);

	@Query(value = "{ 'id' : 'ldap' }")
	LdapConfig findLdap();

	@Query(value = "{ 'id' : 'activeDirectory' }")
	ActiveDirectoryConfig findActiveDirectory();

	@Query(value = "{ 'id' : 'activeDirectory', 'enabled' : ?0 }")
	Optional<ActiveDirectoryConfig> findActiveDirectory(boolean enabled);

}
