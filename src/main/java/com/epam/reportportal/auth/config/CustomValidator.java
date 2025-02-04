package com.epam.reportportal.auth.config;

import org.joda.time.DateTime;
import org.springframework.security.saml.saml2.authentication.*;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;
import org.springframework.security.saml.spi.DefaultValidator;
import org.springframework.security.saml.spi.SpringSecuritySaml;
import org.springframework.security.saml.validation.ValidationResult;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.security.saml.util.DateUtils.toZuluTime;
import static org.springframework.util.StringUtils.hasText;

public class CustomValidator extends DefaultValidator {

    public CustomValidator(SpringSecuritySaml implementation) {
        super(implementation);
    }

    @Override
    protected ValidationResult validate(Response response,
                                        List<String> mustMatchInResponseTo,
                                        ServiceProviderMetadata requester,
                                        IdentityProviderMetadata responder) {
        String entityId = requester.getEntityId();

        if (response == null) {
            return new ValidationResult(response).addError(new ValidationResult.ValidationError("Response is null"));
        }

        if (response.getStatus() == null || response.getStatus().getCode() == null) {
            return new ValidationResult(response).addError(new ValidationResult.ValidationError("Response status or code is null"));
        }

        StatusCode statusCode = response.getStatus().getCode();
        if (statusCode != StatusCode.SUCCESS) {
            return new ValidationResult(response).addError(
                    new ValidationResult.ValidationError("An error response was returned: " + statusCode.toString())
            );
        }

        if (responder == null) {
            return new ValidationResult(response)
                    .addError("Remote provider for response was not found");
        }

        if (response.getSignature() != null && !response.getSignature().isValidated()) {
            return new ValidationResult(response).addError(new ValidationResult.ValidationError("No validated signature present"));
        }

        //verify issue time
        DateTime issueInstant = response.getIssueInstant();
        if (!isDateTimeSkewValid(getResponseSkewTimeMillis(), 0, issueInstant)) {
            return new ValidationResult(response).addError(
                    new ValidationResult.ValidationError("Issue time is either too old or in the future:" + issueInstant.toString())
            );
        }

        //validate InResponseTo
        String replyTo = response.getInResponseTo();
        if (!isAllowUnsolicitedResponses() && !hasText(replyTo)) {
            return new ValidationResult(response).addError(
                    new ValidationResult.ValidationError("InResponseTo is missing and unsolicited responses are disabled")
            );
        }

        if (hasText(replyTo)) {
            if (!isAllowUnsolicitedResponses() && (mustMatchInResponseTo == null || !mustMatchInResponseTo
                    .contains(replyTo))) {
                return new ValidationResult(response).addError(
                        new ValidationResult.ValidationError("Invalid InResponseTo ID, not found in supplied list")
                );
            }
        }

        //validate destination
        if (hasText(response.getDestination()) && !compareURIs(requester.getServiceProvider()
                .getAssertionConsumerService(), response.getDestination())) {
            return new ValidationResult(response).addError(
                    new ValidationResult.ValidationError("Destination mismatch: " + response.getDestination())
            );
        }

        //validate issuer
        //name id if not null should be "urn:oasis:names:tc:SAML:2.0:nameid-format:entity"
        //value should be the entity ID of the responder
        ValidationResult result = verifyIssuer(response.getIssuer(), responder);
        if (result != null) {
            return result;
        }

        boolean requireAssertionSigned = requester.getServiceProvider().isWantAssertionsSigned();
        if (response.getSignature() != null) {
            requireAssertionSigned = requireAssertionSigned && (!response.getSignature().isValidated());
        }


        Assertion validAssertion = null;
        ValidationResult assertionValidation = new ValidationResult(response);
        //DECRYPT ENCRYPTED ASSERTIONS
        for (Assertion assertion : response.getAssertions()) {

            ValidationResult assertionResult = validate(
                    assertion,
                    mustMatchInResponseTo,
                    requester,
                    responder, requireAssertionSigned
            );
            if (!assertionResult.hasErrors()) {
                validAssertion = assertion;
                break;
            }
        }
        if (validAssertion == null) {
            assertionValidation.addError(new ValidationResult.ValidationError("No valid assertion with principal found."));
            return assertionValidation;
        }

        for (AuthenticationStatement statement : ofNullable(validAssertion.getAuthenticationStatements())
                .orElse(emptyList())) {

            if (statement.getSessionNotOnOrAfter() != null && statement.getSessionNotOnOrAfter().isBeforeNow
                    ()) {
                return new ValidationResult(response)
                        .addError(
                                format(
                                        "Authentication session expired on: '%s', current time: '%s'",
                                        toZuluTime(statement.getSessionNotOnOrAfter()),
                                        toZuluTime(new DateTime())
                                )
                        );
            }

            //possibly check the
            //statement.getAuthenticationContext().getClassReference()
        }

        Conditions conditions = validAssertion.getConditions();
        if (conditions != null) {
            //VERIFY conditions
            if (conditions.getNotBefore() != null && conditions.getNotBefore().minusMillis
                    (getResponseSkewTimeMillis()).isAfterNow()) {
                return new ValidationResult(response)
                        .addError("Conditions expired (not before): " + conditions.getNotBefore());
            }

            if (conditions.getNotOnOrAfter() != null && conditions.getNotOnOrAfter().plusMillis
                    (getResponseSkewTimeMillis()).isBeforeNow()) {
                return new ValidationResult(response)
                        .addError("Conditions expired (not on or after): " + conditions.getNotOnOrAfter());
            }

            for (AssertionCondition c : conditions.getCriteria()) {
                if (c instanceof AudienceRestriction) {
                    AudienceRestriction ac = (AudienceRestriction) c;
                    ac.evaluate(entityId, time());
                    if (!ac.isValid()) {
                        return new ValidationResult(response)
                                .addError(
                                        format(
                                                "Audience restriction evaluation failed for assertion condition. Expected '%s' Was '%s'",
                                                entityId,
                                                ac.getAudiences()
                                        )
                                );
                    }
                }
            }
        }

        //the only assertion that we validated - may not be the first one
        response.setAssertions(Arrays.asList(validAssertion));
        return new ValidationResult(response);
    }
}

