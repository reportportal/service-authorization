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

package com.epam.reportportal.auth.rules.commons.exception.forwarding;

import java.io.IOException;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

/**
 * Exception handler for Spring RestTemplate. Throws exception with downstream service response to
 * be forwarded to upstream service (just propagates exception to upstream service)
 *
 * @author Andrei Varabyeu
 */
public class ForwardingClientExceptionHandler extends DefaultResponseErrorHandler {


  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    throw new ResponseForwardingException(response);
  }
}
