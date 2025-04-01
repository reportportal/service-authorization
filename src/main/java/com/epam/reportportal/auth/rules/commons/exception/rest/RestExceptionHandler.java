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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.util.WebUtils;

/**
 * Custom implementation of Spring's error handler.
 *
 * @author Andrei Varabyeu
 */
public class RestExceptionHandler extends DefaultHandlerExceptionResolver {

  /**
   * Error Resolver
   */
  private ErrorResolver errorResolver;

  /**
   * Set of converters to be able to render response.
   */
  private List<HttpMessageConverter<?>> messageConverters;

  public void setErrorResolver(ErrorResolver errorResolver) {
    this.errorResolver = errorResolver;
  }

  public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
    this.messageConverters = messageConverters;
  }


  @Override
  protected ModelAndView doResolveException(HttpServletRequest request,
      HttpServletResponse response, Object handler, Exception ex) {
    LoggerFactory.getLogger(this.getClass()).error("Handled error: ", ex);
    ModelAndView defaultError = super.doResolveException(request, response, handler, ex);
    if (null != defaultError) {
      return defaultError;
    }

    return handleCustomException(request, response, ex);
  }

  protected ModelAndView handleCustomException(HttpServletRequest request,
      HttpServletResponse response, Exception ex) {
    ServletWebRequest webRequest = new ServletWebRequest(request, response);

    RestError error = errorResolver.resolveError(ex);
    if (error == null) {
      return null;
    }
    applyStatusIfPossible(webRequest, error.getHttpStatus());

    try {
      return handleResponseBody(error.getErrorRs(), webRequest);
    } catch (IOException e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Unable to write error message", e);
      }
      return null;
    }
  }

  private void applyStatusIfPossible(ServletWebRequest webRequest, HttpStatus status) {
    if (!WebUtils.isIncludeRequest(webRequest.getRequest())) {
      webRequest.getResponse().setStatus(status.value());
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private ModelAndView handleResponseBody(Object body, ServletWebRequest webRequest)
      throws HttpMessageNotWritableException, IOException {

    HttpInputMessage inputMessage = new ServletServerHttpRequest(webRequest.getRequest());

    List<MediaType> acceptedMediaTypes = inputMessage.getHeaders().getAccept();
    if (acceptedMediaTypes.isEmpty()) {
      acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
    }

    MediaType.sortByQualityValue(acceptedMediaTypes);

    HttpOutputMessage outputMessage = new ServletServerHttpResponse(webRequest.getResponse());

    Class<?> bodyType = body.getClass();

    List<HttpMessageConverter<?>> converters = this.messageConverters;

    if (converters != null) {
      for (MediaType acceptedMediaType : acceptedMediaTypes) {
        for (HttpMessageConverter messageConverter : converters) {
          if (messageConverter.canWrite(bodyType, acceptedMediaType)) {
            messageConverter.write(body, acceptedMediaType, outputMessage);
            // return empty model and view to short circuit the
            // iteration and to let
            // Spring know that we've rendered the view ourselves:
            return new ModelAndView();
          }
        }
      }
    }

    if (logger.isWarnEnabled()) {
      logger.warn(
          "Could not find HttpMessageConverter that supports return type [" + bodyType + "] and "
              + acceptedMediaTypes);
    }
    return null;
  }

  /**
   * Override default behavior and handle bind exception as custom exception
   */
  @Override
  protected ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
      HttpServletRequest request,
      HttpServletResponse response, Object handler) throws IOException {
    return handleCustomException(request, response, ex);
  }

  @Override
  protected ModelAndView handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
      HttpServletRequest request,
      HttpServletResponse response, Object handler) throws IOException {
    return handleCustomException(request, response, ex);
  }

  @Override
  protected ModelAndView handleMissingServletRequestPartException(
      MissingServletRequestPartException ex, HttpServletRequest request,
      HttpServletResponse response, Object handler) throws IOException {
    return handleCustomException(request, response, ex);
  }

  @Override
  protected ModelAndView handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex, HttpServletRequest request,
      HttpServletResponse response, Object handler) throws IOException {
    return handleCustomException(request, response, ex);
  }
}
