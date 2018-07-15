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

import com.epam.reportportal.auth.config.DatabaseConfiguration;
import com.epam.ta.reportportal.dao.ReportPortalRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Application entry point
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
//@SpringBootApplication(exclude = { JpaRepositoriesAutoConfiguration.class })
//@EnableDiscoveryClient

//@Import({ DatabaseConfiguration.class, DatastoreConfiguration.class })
@ComponentScan("com.epam")
@EnableJpaRepositories(basePackages = { "com.epam.ta.reportportal.dao",
		"com.epam.reportportal.auth.store" }, repositoryBaseClass = ReportPortalRepositoryImpl.class, repositoryFactoryBeanClass = DatabaseConfiguration.RpRepoFactoryBean.class)
@SpringBootApplication
public class AuthServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServerApplication.class, args);
	}

}
