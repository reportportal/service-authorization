package com.epam.reportportal.auth.integration.validator.request.param.provider;

import com.epam.reportportal.auth.integration.parameter.LdapParameter;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LdapRequiredParamNamesProvider implements ParamNamesProvider {

	@Override
	public List<String> provide() {
		return Arrays.stream(LdapParameter.values())
				.filter(LdapParameter::isRequired)
				.map(LdapParameter::getParameterName)
				.collect(Collectors.toList());
	}
}
