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
import com.epam.ta.reportportal.entity.integration.Integration;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * AuthConfig repository custom
 *
 * @author Andrei Varabyeu
 */
@Repository
public class AuthConfigRepositoryCustomImpl implements AuthConfigRepositoryCustom {

	//	private final DSLContext dslContext;

	private final EntityManager entityManager;

	public AuthConfigRepositoryCustomImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
		//		createDefaultProfileIfAbsent();
	}

	//
	//    @Override
	//    public void deleteSettings(AuthIntegrationType type) {
	//        mongoOperations.updateFirst(findDefaultQuery(), new Update().unset(type.getDbField()), AuthConfigEntity.class);
	//    }
	//
	//    @Override
	//    public void updatePartially(AuthConfigEntity entity) {
	//        mongoOperations.updateFirst(findDefaultQuery(), updateExisting(entity), AuthConfigEntity.class);
	//    }
	//
	//    @Override
	//    public void updateLdap(LdapConfig ldapConfig) {
	//        mongoOperations
	//                .updateFirst(findDefaultQuery(), Update.update(AuthIntegrationType.LDAP.getDbField(), ldapConfig),
	//                        AuthConfigEntity.class);
	//
	//    }
	//
	//    @Override
	//    public void updateActiveDirectory(ActiveDirectoryConfig adConfig) {
	//        mongoOperations
	//                .updateFirst(findDefaultQuery(),
	//                        Update.update(AuthIntegrationType.ACTIVE_DIRECTORY.getDbField(), adConfig),
	//                        AuthConfigEntity.class);
	//    }

	@Override
	public Optional<LdapConfig> findLdap(boolean enabled) {
		TypedQuery<AuthConfig> authConfigTypedQuery = findDefaultQuery();

		authConfigTypedQuery.setParameter("enabled", enabled);

		return ofNullable(authConfigTypedQuery.getSingleResult()).flatMap(cfg -> ofNullable(cfg.getLdap()));
	}

	@Override
	public Optional<ActiveDirectoryConfig> findActiveDirectory(boolean enabled) {
		TypedQuery<AuthConfig> authConfigTypedQuery = findDefaultQuery();

		return ofNullable(authConfigTypedQuery.getSingleResult()).flatMap(cfg -> ofNullable(cfg.getActiveDirectory()));

		//		return ofNullable(entityManager.createQuery(findDefaultQuery().where(criteriaBuilder
		//				.equal(integrationRoot.get("enabled"), String.valueOf(enabled)))).getSingleResult()).flatMap(cfg -> ofNullable(cfg
		//				.getActiveDirectory()));
	}

	private TypedQuery<AuthConfig> findDefaultQuery() {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<AuthConfig> configCriteriaQuery = criteriaBuilder.createQuery(AuthConfig.class);
		Root<AuthConfig> authConfigRoot = configCriteriaQuery.from(AuthConfig.class);

		Join<AuthConfig, ActiveDirectoryConfig> authConfigIntegrationJoin = authConfigRoot.join("activeDirectory");

		return entityManager.createQuery(configCriteriaQuery.where(criteriaBuilder.equal(authConfigIntegrationJoin.get("enabled"), true), criteriaBuilder.equal(authConfigRoot.get("id"), "default")));
//		return entityManager.createQuery(
//				"SELECT a FROM AuthConfig a JOIN a.activeDirectory ad JOIN Integration i ON ad.id = i.id WHERE a.id = 'default' AND i.enabled = :enabled",
//				AuthConfig.class
//		);
	}

	//    private Update updateExisting(Object object) {
	//        try {
	//            Update update = new Update();
	//            PropertyUtils.describe(object).entrySet().stream().filter(e -> null != e.getValue())
	//                    .forEach(it -> update.set(it.getKey(), it.getValue()));
	//            return update;
	//        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
	//            throw new ReportPortalException("Error during auth config update", e);
	//        }
	//    }

}
