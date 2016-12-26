package com.epam.reportportal.auth;

import com.epam.ta.reportportal.database.dao.ServerSettingsRepository;
import com.epam.ta.reportportal.database.entity.OAuth2LoginDetails;
import com.epam.ta.reportportal.database.entity.ServerSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by andrei_varabyeu on 12/26/16.
 */
@Component
public class DemoDataEvent {

	@Autowired
	private ServerSettingsRepository serverSettingsRepository;

	@EventListener(ContextRefreshedEvent.class)
	public void onStart(){
		ServerSettings settings = serverSettingsRepository.findOne("default");
		settings.setoAuth2LoginDetails(null);

//		Optional<OAuth2LoginDetails> githubDetails = Optional.ofNullable(settings.getoAuth2LoginDetails())
//				.flatMap(details -> details.stream().filter(d -> "github".equals(d.getId())).findAny());
//		if (!githubDetails.isPresent()){
//			OAuth2LoginDetails oAuth2LoginDetails = new OAuth2LoginDetails();
//			oAuth2LoginDetails.setScope(Collections.singletonList("user"));
//			oAuth2LoginDetails.setId("github");
//			oAuth2LoginDetails.setGrantType("authorization_code");
//			oAuth2LoginDetails.setClientId("f4cec43d4541283879c4");
//			oAuth2LoginDetails.setClientSecret("a31aa6de3e27c11d90762cad11936727d6b0759e");
//			oAuth2LoginDetails.setAccessTokenUri("https://github.com/login/oauth/access_token");
//			oAuth2LoginDetails.setUserAuthorizationUri("https://github.com/login/oauth/authorize");
//
//			List<OAuth2LoginDetails> detailsList = Optional.ofNullable(settings.getoAuth2LoginDetails()).orElseGet(ArrayList::new);
//			detailsList.add(oAuth2LoginDetails);
//
//			settings.setoAuth2LoginDetails(detailsList);
//
//		}


		OAuth2LoginDetails oAuth2LoginDetails = new OAuth2LoginDetails();
		oAuth2LoginDetails.setScope(Collections.singletonList("user"));
		oAuth2LoginDetails.setId("github");
		oAuth2LoginDetails.setGrantType("authorization_code");
		oAuth2LoginDetails.setClientId("f4cec43d4541283879c4");
		oAuth2LoginDetails.setClientSecret("a31aa6de3e27c11d90762cad11936727d6b0759e");
		oAuth2LoginDetails.setAccessTokenUri("https://github.com/login/oauth/access_token");
		oAuth2LoginDetails.setUserAuthorizationUri("https://github.com/login/oauth/authorize");
		oAuth2LoginDetails.setClientAuthenticationScheme("form");

		List<OAuth2LoginDetails> detailsList = Optional.ofNullable(settings.getoAuth2LoginDetails()).orElseGet(ArrayList::new);
		detailsList.add(oAuth2LoginDetails);

		settings.setoAuth2LoginDetails(detailsList);

		serverSettingsRepository.save(settings);
	}
}
