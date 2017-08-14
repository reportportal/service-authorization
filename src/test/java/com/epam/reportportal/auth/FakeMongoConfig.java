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
package com.epam.reportportal.auth;

import com.epam.reportportal.auth.store.AuthConfigRepository;
import com.epam.reportportal.auth.util.Encryptor;
import com.github.fakemongo.Fongo;
import com.mongodb.MockMongoClient;
import com.mongodb.WriteConcern;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * @author Andrei Varabyeu
 */
@Configuration
@Profile("unittest")
@EnableMongoRepositories(basePackageClasses = AuthConfigRepository.class)
@ComponentScan(basePackageClasses = { AuthConfigRepository.class, Encryptor.class })
public class FakeMongoConfig {

	@Bean
	@Primary
	public MongoDbFactory mongoDbFactory() {
		final Fongo fongo = new Fongo("InMemoryMongo");
		SimpleMongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(MockMongoClient.create(fongo), "reportportal");
		mongoDbFactory.setWriteConcern(WriteConcern.ACKNOWLEDGED);
		return mongoDbFactory;
	}

	@Bean
	public MongoOperations mongoTemplate() {
		return new MongoTemplate(mongoDbFactory());
	}
}
