package com.epam.reportportal.auth.event;

import com.epam.ta.reportportal.database.entity.settings.SamlProviderDetails;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class SamlProvidersReloadEvent extends ApplicationEvent {

    private static final long serialVersionUID = 2314984509233L;

    public SamlProvidersReloadEvent(Map<String, SamlProviderDetails> externalProviders) {
        super(externalProviders);
    }

    Map<String, SamlProviderDetails> getDetails() {
        return (Map<String, SamlProviderDetails>) super.getSource();
    }
}
