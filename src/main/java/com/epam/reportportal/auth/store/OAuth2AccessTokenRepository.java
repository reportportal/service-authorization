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
package com.epam.reportportal.auth.store;

import com.epam.reportportal.auth.store.entity.OAuth2AccessTokenEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Andrei Varabyeu
 */
@Repository
public interface OAuth2AccessTokenRepository extends MongoRepository<OAuth2AccessTokenEntity, String> {

    OAuth2AccessTokenEntity findByTokenId(String tokenId);

    OAuth2AccessTokenEntity findByRefreshToken(String refreshToken);

    OAuth2AccessTokenEntity findByAuthenticationId(String authenticationId);

    List<OAuth2AccessTokenEntity> findByClientIdAndUserName(String clientId, String userName);

    List<OAuth2AccessTokenEntity> findByClientId(String clientId);

}