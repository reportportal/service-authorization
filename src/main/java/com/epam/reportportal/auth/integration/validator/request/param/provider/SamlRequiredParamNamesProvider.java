package com.epam.reportportal.auth.integration.validator.request.param.provider;

import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SamlRequiredParamNamesProvider implements ParamNamesProvider {

	@Override
	public List<String> provide() {
		return Arrays.stream(SamlParameter.values())
				.filter(SamlParameter::isRequired)
				.map(SamlParameter::getParameterName)
				.collect(Collectors.toList());
	}
}
