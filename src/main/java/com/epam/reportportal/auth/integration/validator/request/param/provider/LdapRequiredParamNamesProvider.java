package com.epam.reportportal.auth.integration.validator.request.param.provider;

import com.epam.reportportal.auth.integration.parameter.LdapParameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

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
