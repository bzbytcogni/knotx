/*
 * Knot.x - Reactive microservice assembler - HTTP Server
 *
 * Copyright (C) 2016 Cognifide Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cognifide.knotx.server;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

public class SupportedMethodsHandler implements Handler<RoutingContext> {

  private KnotxServerConfiguration configuration;

  private SupportedMethodsHandler(KnotxServerConfiguration configuration) {
    this.configuration = configuration;
  }

  public static SupportedMethodsHandler create(KnotxServerConfiguration configuration) {
    return new SupportedMethodsHandler(configuration);
  }

  @Override
  public void handle(RoutingContext context) {
    boolean shouldReject = configuration.getEngineRouting().keySet().stream()
        .noneMatch(supportedMethod -> supportedMethod == context.request().method());

    if (shouldReject) {
      context.fail(HttpResponseStatus.METHOD_NOT_ALLOWED.code());
    } else {
      context.next();
    }
  }
}
