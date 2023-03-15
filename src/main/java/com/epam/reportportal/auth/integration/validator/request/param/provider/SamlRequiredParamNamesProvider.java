package com.epam.reportportal.auth.integration.validator.request.param.provider;

import com.epam.reportportal.auth.integration.parameter.SamlParameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

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
