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

import com.epam.reportportal.auth.store.entity.AuthConfigEntity;
import com.epam.reportportal.auth.store.entity.ldap.ActiveDirectoryConfig;
import com.epam.reportportal.auth.store.entity.ldap.LdapConfig;
import com.epam.ta.reportportal.exception.ReportPortalException;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * AuthConfig repository custom
 *
 * @author Andrei Varabyeu
 */
public class AuthConfigRepositoryImpl implements AuthConfigRepositoryCustom {

	private final MongoOperations mongoOperations;

	@Autowired
	public AuthConfigRepositoryImpl(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
	}

	@Override
	public void createDefaultProfileIfAbsent() {
		if (null == mongoOperations.findOne(findDefaultQuery(), AuthConfigEntity.class)) {
			AuthConfigEntity entity = new AuthConfigEntity();
			entity.setId(AuthConfigRepository.DEFAULT_PROFILE);
			mongoOperations.save(entity);
		}
	}

	@Override
	public void updatePartially(AuthConfigEntity entity) {
		mongoOperations.updateFirst(findDefaultQuery(),  updateExisting(entity), AuthConfigEntity.class);
	}

	@Override
	public void updateLdap(LdapConfig ldapConfig) {
		mongoOperations.updateFirst(findDefaultQuery(), Update.update("ldap", ldapConfig), AuthConfigEntity.class);

	}

	@Override
	public void updateActiveDirectory(ActiveDirectoryConfig adConfig) {
		mongoOperations.updateFirst(findDefaultQuery(), Update.update("activeDirectory", adConfig), AuthConfigEntity.class);
	}

	@Override
	public Optional<LdapConfig> findLdap(boolean enabled) {
		return ofNullable(
				mongoOperations.findOne(findDefaultQuery().addCriteria(Criteria.where("ldap.enabled").is(enabled)), AuthConfigEntity.class))
				.flatMap(cfg -> ofNullable(cfg.getLdap()));
	}

	@Override
	public Optional<ActiveDirectoryConfig> findActiveDirectory(boolean enabled) {
		return ofNullable(mongoOperations
				.findOne(findDefaultQuery().addCriteria(Criteria.where("activeDirectory.enabled").is(enabled)), AuthConfigEntity.class))
				.flatMap(cfg -> ofNullable(cfg.getActiveDirectory()));
	}

	private Query findDefaultQuery() {
		return query(where("_id").is(AuthConfigRepository.DEFAULT_PROFILE));
	}

	private Update updateExisting(Object object) {
		try {
			Update update = new Update();
			PropertyUtils.describe(object).entrySet().stream().filter(e -> null != e.getValue())
					.forEach(it -> update.set(it.getKey(), it.getValue()));
			return update;
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new ReportPortalException("Error during auth config update", e);
		}
	}

}
