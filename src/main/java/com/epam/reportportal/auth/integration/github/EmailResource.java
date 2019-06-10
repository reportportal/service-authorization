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
package com.epam.reportportal.auth.integration.github;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Represents response from GET /user/emails GitHub API
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
class EmailResource {

    public String email;
    public boolean verified;
    public boolean primary;

    public String getEmail() {
        return email;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EmailResource that = (EmailResource) o;
        return verified == that.verified && primary == that.primary && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, verified, primary);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("email", email).add("verified", verified).add("primary", primary).toString();
    }
}
