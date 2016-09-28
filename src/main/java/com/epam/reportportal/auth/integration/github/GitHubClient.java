package com.epam.reportportal.auth.integration.github;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Simple GitHub client
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
public class GitHubClient {

	private static final String GITHUB_BASE_URL = "https://api.github.com";

	private final RestTemplate restTemplate;

	private GitHubClient(String accessToken) {
		this.restTemplate = new RestTemplate();
		this.restTemplate.getInterceptors().add((request, body, execution) -> {
			request.getHeaders().add("Authorization", "bearer " + accessToken);
			return execution.execute(request, body);
		});
	}

	public static GitHubClient withAccessToken(String accessToken) {
		return new GitHubClient(accessToken);
	}

	public UserResource getUser() {
		return this.restTemplate.getForObject(GITHUB_BASE_URL + "/user", UserResource.class);
	}

	public List<EmailResource> getUserEmails() {
		return getForObject(GITHUB_BASE_URL + "/user/emails", new ParameterizedTypeReference<List<EmailResource>>() {
		});
	}

	public ResponseEntity<Resource> downloadResource(String url) {
		return this.restTemplate.getForEntity(url, Resource.class);
	}

	private <T> T getForObject(String url, ParameterizedTypeReference<T> type) {
		return this.restTemplate.exchange(url, HttpMethod.GET, null, type).getBody();
	}
}
