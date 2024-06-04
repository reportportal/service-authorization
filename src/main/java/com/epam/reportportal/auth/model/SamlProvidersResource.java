package com.epam.reportportal.auth.model;

import com.epam.ta.reportportal.ws.model.integration.auth.AbstractAuthResource;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import java.util.List;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SamlProvidersResource extends AbstractAuthResource {
    @Valid
    private List<SamlResource> providers;
}
