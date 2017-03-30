/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.auth;

import com.epam.ta.reportportal.database.entity.user.UserRole;
import com.google.common.io.BaseEncoding;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Andrei Varabyeu
 */
public class AuthUtilsTest {

    private static final String OAUTH_OBJECT = "rO0ABXNyAEFvcmcuc3ByaW5nZnJhbWV3b3JrLnNlY3VyaXR5Lm9hdXRoMi5wcm92aWRlci5PQXV0aDJBdXRoZW50aWNhdGlvbr1ACwIWYlITAgACTAANc3RvcmVkUmVxdWVzdHQAPExvcmcvc3ByaW5nZnJhbWV3b3JrL3NlY3VyaXR5L29hdXRoMi9wcm92aWRlci9PQXV0aDJSZXF1ZXN0O0wAEnVzZXJBdXRoZW50aWNhdGlvbnQAMkxvcmcvc3ByaW5nZnJhbWV3b3JrL3NlY3VyaXR5L2NvcmUvQXV0aGVudGljYXRpb247eHIAR29yZy5zcHJpbmdmcmFtZXdvcmsuc2VjdXJpdHkuYXV0aGVudGljYXRpb24uQWJzdHJhY3RBdXRoZW50aWNhdGlvblRva2Vu06oofm5HZA4CAANaAA1hdXRoZW50aWNhdGVkTAALYXV0aG9yaXRpZXN0ABZMamF2YS91dGlsL0NvbGxlY3Rpb247TAAHZGV0YWlsc3QAEkxqYXZhL2xhbmcvT2JqZWN0O3hwAHNyACZqYXZhLnV0aWwuQ29sbGVjdGlvbnMkVW5tb2RpZmlhYmxlTGlzdPwPJTG17I4QAgABTAAEbGlzdHQAEExqYXZhL3V0aWwvTGlzdDt4cgAsamF2YS51dGlsLkNvbGxlY3Rpb25zJFVubW9kaWZpYWJsZUNvbGxlY3Rpb24ZQgCAy173HgIAAUwAAWNxAH4ABHhwc3IAE2phdmEudXRpbC5BcnJheUxpc3R4gdIdmcdhnQMAAUkABHNpemV4cAAAAAF3BAAAAAFzcgBCb3JnLnNwcmluZ2ZyYW1ld29yay5zZWN1cml0eS5jb3JlLmF1dGhvcml0eS5TaW1wbGVHcmFudGVkQXV0aG9yaXR5AAAAAAAAAZoCAAFMAARyb2xldAASTGphdmEvbGFuZy9TdHJpbmc7eHB0AAlST0xFX1VTRVJ4cQB+AAxwc3IAOm9yZy5zcHJpbmdmcmFtZXdvcmsuc2VjdXJpdHkub2F1dGgyLnByb3ZpZGVyLk9BdXRoMlJlcXVlc3QAAAAAAAAAAQIAB1oACGFwcHJvdmVkTAALYXV0aG9yaXRpZXNxAH4ABEwACmV4dGVuc2lvbnN0AA9MamF2YS91dGlsL01hcDtMAAtyZWRpcmVjdFVyaXEAfgAOTAAHcmVmcmVzaHQAO0xvcmcvc3ByaW5nZnJhbWV3b3JrL3NlY3VyaXR5L29hdXRoMi9wcm92aWRlci9Ub2tlblJlcXVlc3Q7TAALcmVzb3VyY2VJZHN0AA9MamF2YS91dGlsL1NldDtMAA1yZXNwb25zZVR5cGVzcQB+ABR4cgA4b3JnLnNwcmluZ2ZyYW1ld29yay5zZWN1cml0eS5vYXV0aDIucHJvdmlkZXIuQmFzZVJlcXVlc3Q2KHo+o3FpvQIAA0wACGNsaWVudElkcQB+AA5MABFyZXF1ZXN0UGFyYW1ldGVyc3EAfgASTAAFc2NvcGVxAH4AFHhwdAADYXBpc3IAJWphdmEudXRpbC5Db2xsZWN0aW9ucyRVbm1vZGlmaWFibGVNYXDxpaj+dPUHQgIAAUwAAW1xAH4AEnhwc3IAEWphdmEudXRpbC5IYXNoTWFwBQfawcMWYNEDAAJGAApsb2FkRmFjdG9ySQAJdGhyZXNob2xkeHA/QAAAAAAABncIAAAACAAAAAR0AAVncmFudHQACHBhc3N3b3JkdAAKZ3JhbnRfdHlwZXB0AAljbGllbnRfaWRxAH4AF3QACHVzZXJuYW1ldAAGbGVhZC0xeHNyACVqYXZhLnV0aWwuQ29sbGVjdGlvbnMkVW5tb2RpZmlhYmxlU2V0gB2S0Y+bgFUCAAB4cQB+AAlzcgAXamF2YS51dGlsLkxpbmtlZEhhc2hTZXTYbNdald0qHgIAAHhyABFqYXZhLnV0aWwuSGFzaFNldLpEhZWWuLc0AwAAeHB3DAAAABA/QAAAAAAAAXEAfgAXeAFzcQB+ACV3DAAAABA/QAAAAAAAAHhzcQB+ABo/QAAAAAAAAHcIAAAAEAAAAAB4cHBzcQB+ACV3DAAAABA/QAAAAAAAAHhzcQB+ACV3DAAAABA/QAAAAAAAAHhzcgBPb3JnLnNwcmluZ2ZyYW1ld29yay5zZWN1cml0eS5hdXRoZW50aWNhdGlvbi5Vc2VybmFtZVBhc3N3b3JkQXV0aGVudGljYXRpb25Ub2tlbgAAAAAAAAGaAgACTAALY3JlZGVudGlhbHNxAH4ABUwACXByaW5jaXBhbHEAfgAFeHEAfgADAXNxAH4AB3NxAH4ACwAAAAF3BAAAAAFxAH4AD3hxAH4ALnNyABdqYXZhLnV0aWwuTGlua2VkSGFzaE1hcDTATlwQbMD7AgABWgALYWNjZXNzT3JkZXJ4cQB+ABo/QAAAAAAADHcIAAAAEAAAAAJ0AApncmFudF90eXBldAAIcGFzc3dvcmR0AAh1c2VybmFtZXQABmxlYWQtMXgAcHNyADJvcmcuc3ByaW5nZnJhbWV3b3JrLnNlY3VyaXR5LmNvcmUudXNlcmRldGFpbHMuVXNlcgAAAAAAAAGaAgAHWgARYWNjb3VudE5vbkV4cGlyZWRaABBhY2NvdW50Tm9uTG9ja2VkWgAVY3JlZGVudGlhbHNOb25FeHBpcmVkWgAHZW5hYmxlZEwAC2F1dGhvcml0aWVzcQB+ABRMAAhwYXNzd29yZHEAfgAOTAAIdXNlcm5hbWVxAH4ADnhwAQEBAXNxAH4AInNyABFqYXZhLnV0aWwuVHJlZVNldN2YUJOV7YdbAwAAeHBzcgBGb3JnLnNwcmluZ2ZyYW1ld29yay5zZWN1cml0eS5jb3JlLnVzZXJkZXRhaWxzLlVzZXIkQXV0aG9yaXR5Q29tcGFyYXRvcgAAAAAAAAGaAgAAeHB3BAAAAAFxAH4AD3hwcQB+ACE=";

    @Test
    public void testAuthoritiesConverter() {
        Collection<GrantedAuthority> grantedAuthorities = Collections.singletonList(UserRole.USER).stream()
                .map(AuthUtils.AS_AUTHORITIES)
                .collect(Collectors.toList()).get(0);
        Assert.assertThat("Incorrect authority conversion", grantedAuthorities.iterator().next().getAuthority(),
                Matchers.is(UserRole.USER.getAuthority()));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeserializerVersionMismatch() {
        SerializationUtils.deserialize(BaseEncoding.base64().decode(OAUTH_OBJECT));
    }

    @Test
    public void testSaveDeserializer() {
        AuthUtils.deserializeSafely(BaseEncoding.base64().decode(OAUTH_OBJECT), new Consumer<OAuth2Authentication>() {
            @Override
            public void accept(OAuth2Authentication oauth) {
                Assert.assertEquals("lead-1", oauth.getName());
            }
        });
    }
}
