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

package com.epam.reportportal.auth.entity.project;

import java.io.Serializable;

/**
 * @author Pavel Bortnik
 */
public class ProjectAnalyzerConfig implements Serializable {

  public static final int MIN_DOC_FREQ = 1;
  public static final int MIN_TERM_FREQ = 1;
  public static final int MIN_SHOULD_MATCH = 95;
  public static final int NUMBER_OF_LOG_LINES = -1;

}
