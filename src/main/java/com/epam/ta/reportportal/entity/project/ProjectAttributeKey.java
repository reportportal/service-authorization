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

package com.epam.ta.reportportal.entity.project;

import com.epam.ta.reportportal.entity.attribute.Attribute;
import java.io.Serializable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author Andrey Plisunov
 */
@Setter
@Getter
public class ProjectAttributeKey implements Serializable {

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "project_id", nullable = false, insertable = false, updatable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "attribute_id", nullable = false, insertable = false, updatable = false)
  private Attribute attribute;

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(project).append(attribute).toHashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ProjectAttributeKey)) {
      return false;
    }

    ProjectAttributeKey projectAttributeKey = (ProjectAttributeKey) obj;

    return new EqualsBuilder().append(project.getId(), projectAttributeKey.project.getId())
        .append(attribute.getId(), projectAttributeKey.attribute.getId())
        .isEquals();
  }
}
