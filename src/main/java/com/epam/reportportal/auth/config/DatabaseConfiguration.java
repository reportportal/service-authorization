/*
 * Copyright 2017 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-api
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

package com.epam.reportportal.auth.config;

import com.epam.ta.reportportal.dao.ReportPortalRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Pavel Bortnik
 */
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = { "com.epam.ta.reportportal.dao",
		"com.epam.reportportal.auth.store" }, repositoryBaseClass = ReportPortalRepositoryImpl.class, repositoryFactoryBeanClass = DatabaseConfiguration.RpRepoFactoryBean.class)
@EnableTransactionManagement
public class DatabaseConfiguration {

	@Autowired
	private DataSourceProperties properties;

	@Bean
	public DataSource dataSource() {
		return properties.initializeDataSourceBuilder().build();
	}

	@Bean
	public EntityManagerFactory entityManagerFactory() {

		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		vendorAdapter.setGenerateDdl(true);

		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setJpaVendorAdapter(vendorAdapter);
		factory.setPackagesToScan(
				"com.epam.reportportal.auth.store",
				"com.epam.ta.reportportal.commons",
				"com.epam.ta.reportportal.entity"
		);
		factory.setDataSource(dataSource());

		Properties jpaProperties = new Properties();
		jpaProperties.setProperty("hibernate.dialect", "com.epam.ta.reportportal.commons.JsonbAwarePostgresDialect");
		factory.setJpaProperties(jpaProperties);

		factory.afterPropertiesSet();

		return factory.getObject();
	}

	public static class RpRepoFactoryBean<T extends Repository<S, ID>, S, ID> extends JpaRepositoryFactoryBean {

		@Autowired
		private AutowireCapableBeanFactory beanFactory;

		/**
		 * Creates a new {@link JpaRepositoryFactoryBean} for the given repository interface.
		 *
		 * @param repositoryInterface must not be {@literal null}.
		 */
		public RpRepoFactoryBean(Class repositoryInterface) {
			super(repositoryInterface);
		}

		@Override
		protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
			return new JpaRepositoryFactory(entityManager) {
				@Override
				public <T> T getRepository(Class<T> repositoryInterface) {
					T repo = super.getRepository(repositoryInterface);
					beanFactory.autowireBean(repo);
					return repo;
				}

				@Override
				protected Object getTargetRepository(RepositoryInformation information) {
					Object repo = super.getTargetRepository(information);
					beanFactory.autowireBean(repo);
					return repo;
				}
			};
		}
	}

	@Bean
	public TransactionAwareDataSourceProxy transactionAwareDataSource() {
		return new TransactionAwareDataSourceProxy(dataSource());
	}

	@Bean
	public DataSourceTransactionManager transactionManager() {
		return new DataSourceTransactionManager(dataSource());
	}

}
