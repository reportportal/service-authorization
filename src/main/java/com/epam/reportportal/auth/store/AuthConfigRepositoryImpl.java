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
import com.epam.ta.reportportal.commons.querygen.Filter;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * AuthConfig repository custom
 *
 * @author Andrei Varabyeu
 */
@Repository
@Transactional
public class AuthConfigRepositoryImpl implements AuthConfigRepositoryCustom {

	//	private final DSLContext dslContext;

	private final EntityManager entityManager;

	@Autowired
	public AuthConfigRepositoryImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
		//		createDefaultProfileIfAbsent();
	}

	@Override
	@Transactional
	public void refresh(AuthConfig t) {
		entityManager.refresh(t);
	}

	@Override
	public <R> List<R> findByFilter(Filter filter, RecordMapper<? super Record, R> mapper) {
		return null;
	}

	@Override
	public <R> Page<R> findByFilter(Filter filter, Pageable pageable, RecordMapper<? super Record, R> mapper) {
		return null;
	}

	@Override
	public boolean exists(Filter filter) {
		return false;
	}

	@Override
	public void createDefaultProfileIfAbsent() {
		List<AuthConfig> authConfigs = entityManager.createQuery(findDefaultQuery()).getResultList();
		if (null == authConfigs || authConfigs.size() == 0) {
			AuthConfig entity = new AuthConfig();
			entity.setId(AuthConfigRepository.DEFAULT_PROFILE);
			entityManager.persist(entity);
		}
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
		CriteriaQuery<AuthConfig> authConfigCriteriaQuery = findDefaultQuery();
		Root<AuthConfig> authConfigRoot = authConfigCriteriaQuery.from(AuthConfig.class);
		return ofNullable(entityManager.createQuery(findDefaultQuery().where(entityManager.getCriteriaBuilder()
				.equal(authConfigRoot.get("ldap.enabled"), String.valueOf(enabled))))
				.getSingleResult()).flatMap(cfg -> ofNullable(cfg.getLdap()));
	}

	@Override
	public Optional<ActiveDirectoryConfig> findActiveDirectory(boolean enabled) {
		CriteriaQuery<AuthConfig> authConfigCriteriaQuery = findDefaultQuery();
		Root<AuthConfig> authConfigRoot = authConfigCriteriaQuery.from(AuthConfig.class);
		return ofNullable(entityManager.createQuery(findDefaultQuery().where(entityManager.getCriteriaBuilder()
				.equal(authConfigRoot.get("active_directory_config.enabled"), String.valueOf(enabled))))
				.getSingleResult()).flatMap(cfg -> ofNullable(cfg.getActiveDirectory()));
	}

	private CriteriaQuery<AuthConfig> findDefaultQuery() {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<AuthConfig> authConfigCriteriaQuery = criteriaBuilder.createQuery(AuthConfig.class);
		Root<AuthConfig> authConfigRoot = authConfigCriteriaQuery.from(AuthConfig.class);
		authConfigCriteriaQuery.select(authConfigRoot).where(criteriaBuilder.equal(authConfigRoot.get("id"), "default"));
		return authConfigCriteriaQuery;
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
