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
package com.epam.reportportal.auth.integration.converter;

import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistrationRestriction;
import com.epam.ta.reportportal.ws.model.settings.OAuthRegistrationResource;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Converter between database and resource representation of OAuthRegistration restrictions.
 *
 * @author Anton Machulski
 */
public class OAuthRestrictionConverter {

	private static final String ORGANIZATIONS_KEY = "organizations";
	//TODO merge with ORGANIZATION_KEY to provide dynamic restrictions (remove both constants and allow to set values from the resource),
	// UI-form refactoring required. Restriction type will be set as `key` and values will be passed in the `values` section (replace String
	// by Set<String> to allow multiple values instead of String splitting
	private static final String ORGANIZATION_TYPE = "organization";

	public final static Function<OAuthRegistrationResource, List<OAuthRegistrationRestriction>> FROM_RESOURCE = OAuthRestrictionConverter::organizationsFromResource;

	public final static Function<OAuthRegistration, Map<String, String>> TO_RESOURCE = db -> {
		Map<String, String> restrictions = new HashMap<>();
		restrictions.put(ORGANIZATIONS_KEY, organizationsToResource(db));
		return restrictions;
	};

	private static List<OAuthRegistrationRestriction> organizationsFromResource(OAuthRegistrationResource resource) {
		return ofNullable(resource.getRestrictions()).flatMap(restrictions -> ofNullable(restrictions.get(ORGANIZATIONS_KEY)))
				.map(it -> Splitter.on(",").omitEmptyStrings().splitToList(it))
				.orElseGet(Lists::newArrayList)
				.stream()
				.map(organization -> {
					OAuthRegistrationRestriction restriction = new OAuthRegistrationRestriction();
					restriction.setType(ORGANIZATION_TYPE);
					restriction.setValue(organization);
					return restriction;
				})
				.collect(Collectors.toList());
	}

	private static String organizationsToResource(OAuthRegistration db) {
		return db.getRestrictions()
				.stream()
				.filter(restriction -> ORGANIZATION_TYPE.equalsIgnoreCase(restriction.getType()))
				.map(OAuthRegistrationRestriction::getValue)
				.collect(Collectors.joining(","));
	}
}
