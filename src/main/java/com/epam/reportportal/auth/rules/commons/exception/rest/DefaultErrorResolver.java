/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.auth.rules.commons.exception.rest;

import static com.epam.reportportal.auth.rules.commons.validation.Suppliers.formattedMessage;
import static com.epam.reportportal.auth.rules.commons.validation.Suppliers.formattedSupplier;

import com.google.common.base.Preconditions;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Default implementation of ErrorResolver.
 *
 * @author Andrei Varabyeu
 */
public class DefaultErrorResolver implements ErrorResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultErrorResolver.class);

  private final Map<Class<? extends Throwable>, RestErrorDefinition> exceptionMappingDefinitions;

  public DefaultErrorResolver(
      Map<Class<? extends Throwable>, RestErrorDefinition> exceptionMappingDefinitions) {
    Preconditions
        .checkNotNull(exceptionMappingDefinitions, "Exceptions mappings should't be null");
    this.exceptionMappingDefinitions = exceptionMappingDefinitions;
  }


  @Override
  public RestError resolveError(Exception ex) {
    RestErrorDefinition errorDefinition = getRestErrorDefinition(ex);
    if (null == errorDefinition) {
      return null;
    }

    LOGGER.error(ex.getMessage(), ex);

    RestError.Builder errorBuilder = new RestError.Builder();
    String message;
    if (formattedMessage(errorDefinition.getError().getDescription())) {
      message = formattedSupplier(errorDefinition.getError().getDescription(),
          errorDefinition.getExceptionMessage(ex)).get();
    } else {
      message = new StringBuilder(errorDefinition.getError().getDescription()).append(" [")
          .append(errorDefinition.getExceptionMessage(ex)).append("]").toString();
    }
    errorBuilder.setError(errorDefinition.getError())
        .setMessage(message)
        .setStatus(errorDefinition.getHttpStatus());

    return errorBuilder.build();

  }

  /**
   * Returns the config-time 'template' RestErrorDefinition instance configured for the specified
   * Exception, or {@code null} if a match was not found.
   * <p/>
   * The config-time template is used as the basis for the RestError constructed at runtime.
   *
   * @param ex Exception to be resolved
   * @return the template to use for the RestError instance to be constructed.
   */
  private RestErrorDefinition getRestErrorDefinition(Exception ex) {
    Map<Class<? extends Throwable>, RestErrorDefinition> mappings = this.exceptionMappingDefinitions;
    if (mappings.isEmpty()) {
      return null;
    }
    RestErrorDefinition template = null;
    int deepest = Integer.MAX_VALUE;
    for (Map.Entry<Class<? extends Throwable>, RestErrorDefinition> entry : mappings.entrySet()) {
      Class<? extends Throwable> key = entry.getKey();
      int depth = getDepth(key, ex);
      if (depth >= 0 && depth < deepest) {
        deepest = depth;
        template = entry.getValue();
      }
    }
    return template;
  }

  /**
   * Return the depth to the superclass matching.
   *
   * @param exceptionMapping Possible exception match
   * @param ex               Exception to be checked
   * @return 0 means ex matches exactly. Returns -1 if there's no match. Otherwise, returns depth.
   * Lowest depth wins.
   */
  protected int getDepth(Class<? extends Throwable> exceptionMapping, Exception ex) {
    return getDepth(exceptionMapping, ex.getClass(), 0);
  }

  private int getDepth(Class<? extends Throwable> exceptionMapping, Class<?> exceptionClass,
      int depth) {
    if (exceptionClass.equals(exceptionMapping)) {
      // Found it!
      return depth;
    }
    // If we've gone as far as we can go and haven't found it...
    if (exceptionClass.equals(Throwable.class)) {
      return -1;
    }
    return getDepth(exceptionMapping, exceptionClass.getSuperclass(), depth + 1);
  }

}
