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

import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Shows list of supported authentication providers
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class AuthProvidersInfoContributor implements InfoContributor {

	private final ServerSettingsRepository settingsRepository;

	@Autowired
	public AuthProvidersInfoContributor(ServerSettingsRepository settingsRepository) {
		this.settingsRepository = settingsRepository;
	}

	@Override
	public void contribute(Info.Builder builder) {
		Optional<Set<String>> loginDetails = Optional.ofNullable(settingsRepository.findOne("default"))
				.flatMap(settings -> Optional.ofNullable(settings.getoAuth2LoginDetails())).map(Map::keySet);
		loginDetails.ifPresent(it -> builder.withDetail("auth_extensions", it));
	}
}
