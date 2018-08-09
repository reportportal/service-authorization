package com.epam.reportportal.auth.converter;

import com.epam.ta.reportportal.entity.oauth.OAuthRegistration;
import com.epam.ta.reportportal.entity.oauth.OAuthRegistrationRestriction;
import com.epam.ta.reportportal.ws.model.settings.OAuthRegistrationResource;
import com.google.common.base.Splitter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

class OAuthRestrictionConverter {
	public final static Function<OAuthRegistrationResource, List<OAuthRegistrationRestriction>> FROM_RESOURCE = resource -> {
		List<OAuthRegistrationRestriction> restrictions = organizationsFromResource(resource);
		return restrictions;
	};

	public final static Function<OAuthRegistration, Map<String, String>> TO_RESOURCE = db -> {
		Map<String, String> restrictions = new HashMap<>();
		restrictions.put("organizations", organizationsToResource(db));
		return restrictions;
	};

	private static List<OAuthRegistrationRestriction> organizationsFromResource(OAuthRegistrationResource resource) {
		return ofNullable(resource.getRestrictions()).flatMap(restrictions -> ofNullable(restrictions.get("organizations")))
				.map(it -> Splitter.on(",").omitEmptyStrings().splitToList(it))
				.orElse(new ArrayList<>())
				.stream()
				.map(organization -> {
					OAuthRegistrationRestriction restriction = new OAuthRegistrationRestriction();
					restriction.setType("organization");
					restriction.setValue(organization);
					return restriction;
				})
				.collect(Collectors.toList());
	}

	private static String organizationsToResource(OAuthRegistration db) {
		return db.getRestrictions()
				.stream()
				.filter(restriction -> "organization".equalsIgnoreCase(restriction.getType()))
				.map(OAuthRegistrationRestriction::getValue)
				.collect(Collectors.joining(","));
	}
}
