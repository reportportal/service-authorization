package com.epam.reportportal.auth.integration.saml;

import com.epam.reportportal.auth.integration.AbstractUserReplicator;
import com.epam.ta.reportportal.database.DataStorage;
import com.epam.ta.reportportal.database.dao.ProjectRepository;
import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.dao.UserRepository;
import com.epam.ta.reportportal.database.entity.settings.SamlProviderDetails;
import com.epam.ta.reportportal.database.entity.settings.ServerSettings;
import com.epam.ta.reportportal.database.entity.user.User;
import com.epam.ta.reportportal.database.entity.user.UserRole;
import com.epam.ta.reportportal.database.entity.user.UserType;
import com.epam.ta.reportportal.database.personal.PersonalProjectService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.ta.reportportal.commons.EntityUtils.normalizeId;

@Component
public class SamlUserReplicator extends AbstractUserReplicator {

    private ServerSettingsRepository serverSettingsRepository;

    public SamlUserReplicator(UserRepository userRepository, ProjectRepository projectRepository,
                              PersonalProjectService personalProjectService, DataStorage dataStorage,
                              ServerSettingsRepository serverSettingsRepository) {
        super(userRepository, projectRepository, personalProjectService, dataStorage);
        this.serverSettingsRepository = serverSettingsRepository;
    }

    public User replicateUser(ReportPortalSamlAuthentication samlAuthentication) {
        String userName = normalizeId(samlAuthentication.getPrincipal());
        User user = userRepository.findOne(userName);

        if (user == null) {

            ServerSettings serverSettings = serverSettingsRepository.findOne("default");
            Map<String, SamlProviderDetails> samlProviders = Optional.ofNullable(serverSettings.getSamlProviderDetails()).orElseGet(HashMap::new);
            Optional<SamlProviderDetails> first = samlProviders.values().stream()
                    .filter(provider -> provider.getIdpUrl().equalsIgnoreCase(samlAuthentication.getIssuer()))
                    .findFirst();

            user = new User();
            user.setLogin(userName);

            List<Attribute> details = samlAuthentication.getDetails();

            if (first.isPresent()) {
                populateUserDetailsIfSettingsArePresent(user, first.get(), details);
            } else {
                populateUserDetails(user, details);
            }

            user.setType(UserType.SAML);
            user.setRole(UserRole.USER);
            user.setIsExpired(false);

            user.setDefaultProject(generatePersonalProject(user));
            user.setMetaInfo(defaultMetaInfo());

            userRepository.save(user);
        }

        return user;
    }

    private void populateUserDetails(User user, List<Attribute> details) {
        String email = findAttributeValue(details, UserAttribute.EMAIL.toString(), String.class);
        checkEmail(email);
        user.setEmail(email);

        String firstName = findAttributeValue(details, UserAttribute.FIRST_NAME.toString(), String.class);
        String lastName = findAttributeValue(details, UserAttribute.LAST_NAME.toString(), String.class);
        user.setFullName(String.join(" ", firstName, lastName));
    }

    private void populateUserDetailsIfSettingsArePresent(User user, SamlProviderDetails providerDetails, List<Attribute> details) {
        String email = findAttributeValue(details, providerDetails.getEmailAttributeId(), String.class);
        checkEmail(email);
        user.setEmail(email);

        if (StringUtils.isEmpty(providerDetails.getFullNameAttributeId())) {
            String firstName = findAttributeValue(details, providerDetails.getFirstNameAttributeId(), String.class);
            String lastName = findAttributeValue(details, providerDetails.getLastNameAttributeId(), String.class);
            user.setFullName(String.join(" ", firstName, lastName));
        } else {
            String fullName = findAttributeValue(details, providerDetails.getFullNameAttributeId(), String.class);
            user.setFullName(fullName);
        }
    }

    private <T> T findAttributeValue(List<Attribute> attributes, String lookingFor, Class<T> castTo) {
        if (CollectionUtils.isEmpty(attributes)) {
            return null;
        }

        Optional<Attribute> attribute = attributes.stream()
                .filter(it -> it.getName().equalsIgnoreCase(lookingFor))
                .findFirst();

        if (attribute.isPresent()) {
            List<Object> values = attribute.get().getValues();
            if (!CollectionUtils.isEmpty(values)) {
                List<T> resultList = values.stream().filter(castTo::isInstance)
                        .map(castTo::cast)
                        .collect(Collectors.toList());
                if (!resultList.isEmpty()) {
                    return resultList.get(0);
                }
            }
        }
        return null;
    }
}
