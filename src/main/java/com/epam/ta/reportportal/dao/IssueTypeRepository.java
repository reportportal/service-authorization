/*
 * Copyright 2024 EPAM Systems
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

package com.epam.ta.reportportal.dao;


import com.epam.ta.reportportal.entity.item.issue.IssueType;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

/**
 * @author Siarhei Hrabko
 */
public interface IssueTypeRepository extends ReportPortalRepository<IssueType, Long> {

  @Query(value = "SELECT from issue_type it join public.issue_group ig on it.issue_group_id = ig.issue_group_id where it.locator in (:TestItemIssueGroup.values())", nativeQuery = true)
  List<IssueType> getDefaultIssueTypes();

}
