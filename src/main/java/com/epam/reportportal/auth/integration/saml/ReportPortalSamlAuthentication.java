/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.auth.integration.saml;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.saml.SamlAuthentication;
import org.springframework.security.saml.saml2.authentication.Assertion;
import org.springframework.security.saml.saml2.authentication.SubjectPrincipal;
import org.springframework.security.saml.spi.DefaultSamlAuthentication;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.reportportal.auth.util.AuthUtils.CROP_DOMAIN;

/**
 * Information extracted from SAML response
 *
 * @author Yevgeniy Svalukhin
 */
public class ReportPortalSamlAuthentication implements SamlAuthentication {

	private static final long serialVersionUID = -289812989450932L;

	private boolean authenticated;
	private Subject subject;
	private List<Attribute> attributes = new LinkedList<>();
	private List<? extends GrantedAuthority> grantedAuthorities;
	private transient Assertion assertion;
	private String assertingEntityId;
	private String holdingEntityId;
	private String relayState;
	private String responseXml;
	private String issuer;

	public ReportPortalSamlAuthentication(boolean authenticated, Assertion assertion, String assertingEntityId, String holdingEntityId,
			String relayState) {
		this.authenticated = authenticated;
		this.assertingEntityId = assertingEntityId;
		this.holdingEntityId = holdingEntityId;
		this.relayState = relayState;
		this.assertion = assertion;
		fillSubject(assertion);
		fillAttributes(assertion);
		issuer = assertion.getIssuer().getValue();
	}

	public ReportPortalSamlAuthentication(DefaultSamlAuthentication defaultSamlAuthentication) {
		this(
				defaultSamlAuthentication.isAuthenticated(),
				defaultSamlAuthentication.getAssertion(),
				defaultSamlAuthentication.getAssertingEntityId(),
				defaultSamlAuthentication.getHoldingEntityId(),
				defaultSamlAuthentication.getRelayState()
		);
	}

	private void fillAttributes(Assertion assertion) {
		List<Attribute> mappedAttributes = assertion.getAttributes()
				.stream()
				.map(attr -> new Attribute().setName(attr.getName())
						.setFriendlyName(attr.getFriendlyName())
						.setNameFormat(attr.getNameFormat().toString())
						.setRequired(attr.isRequired())
						.setValues(attr.getValues()))
				.collect(Collectors.toList());
		attributes.addAll(mappedAttributes);
	}

	private void fillSubject(Assertion assertion) {
		subject = new Subject().setSamlPrincipal(new SamlPrincipal().setFormat(assertion.getSubject()
				.getPrincipal()
				.getFormat()
				.getFormat()
				.toString()).setValue(CROP_DOMAIN.apply(assertion.getSubject().getPrincipal().getValue())));
	}

	@Override
	public String getAssertingEntityId() {
		return assertingEntityId;
	}

	@Override
	public String getHoldingEntityId() {
		return holdingEntityId;
	}

	@Override
	public SubjectPrincipal<? extends SubjectPrincipal> getSamlPrincipal() {
		return subject.getSamlPrincipal();
	}

	@Override
	public Assertion getAssertion() {
		return assertion;
	}

	@Override
	public String getRelayState() {
		return relayState;
	}

	public void setRelayState(String relayState) {
		this.relayState = relayState;
	}

	protected void setHoldingEntityId(String holdingEntityId) {
		this.holdingEntityId = holdingEntityId;
	}

	protected void setAssertingEntityId(String assertingEntityId) {
		this.assertingEntityId = assertingEntityId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return grantedAuthorities;
	}

	@Override
	public Subject getCredentials() {
		return subject;
	}

	@Override
	public List<Attribute> getDetails() {
		return attributes;
	}

	@Override
	public String getPrincipal() {
		return subject.getSamlPrincipal().getValue();
	}

	@Override
	public boolean isAuthenticated() {
		return authenticated;
	}

	@Override
	public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
		if (!authenticated && isAuthenticated) {
			throw new IllegalArgumentException("Unable to change state of an existing authentication object.");
		}
	}

	@Override
	public String getName() {
		return subject.getSamlPrincipal().getName();
	}

	public String getResponseXml() {
		return responseXml;
	}

	public ReportPortalSamlAuthentication setResponseXml(String responseXml) {
		this.responseXml = responseXml;
		return this;
	}

	public void setAuthorities(List<? extends GrantedAuthority> grantedAuthorities) {
		this.grantedAuthorities = grantedAuthorities;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}
}
