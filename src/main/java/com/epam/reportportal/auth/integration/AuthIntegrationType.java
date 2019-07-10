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
package com.epam.reportportal.auth.integration;

import com.epam.ta.reportportal.entity.integration.Integration;

import java.util.Arrays;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * @author Andrei Varabyeu
 */
public enum AuthIntegrationType {

    ACTIVE_DIRECTORY("ad", "activeDirectory") {
        @Override
        public Optional<Integration> get(Integration entity) {
            return ofNullable(entity);
        }
    },
    LDAP("ldap", "ldap") {
        @Override
        public Optional<Integration> get(Integration entity) {
            return ofNullable(entity);
        }
    };

    private String id;
    private String dbField;

    AuthIntegrationType(String id, String dbField) {
        this.id = id;
        this.dbField = dbField;
    }

    public abstract Optional<Integration> get(Integration entity);

    public String getId() {
        return id;
    }

    public String getDbField() {
        return dbField;
    }

    public static Optional<AuthIntegrationType> fromId(String id) {
        return Arrays.stream(values()).filter(it -> it.id.equalsIgnoreCase(id)).findAny();
    }

    @Override
    public String toString() {
        return this.id;
    }
}
