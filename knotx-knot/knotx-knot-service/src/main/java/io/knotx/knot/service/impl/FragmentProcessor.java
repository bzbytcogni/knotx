/*
 * Copyright (C) 2016 Cognifide Limited
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
package io.knotx.knot.service.impl;

import io.knotx.dataobjects.Fragment;
import io.knotx.dataobjects.KnotContext;
import io.knotx.dataobjects.KnotStatus;
import io.knotx.knot.service.ServiceKnotOptions;
import io.knotx.knot.service.service.ServiceEngine;
import io.knotx.knot.service.service.ServiceEntry;
import io.knotx.util.FragmentUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import java.util.concurrent.ExecutionException;

/**
 * @deprecated  As of release 1.3.1, replaced by <a href="https://github.com/Knotx/knotx-data-bridge/blob/master/core/src/main/java/io/knotx/databridge/core/impl/FragmentProcessor.java">FragmentProcessor</a>
 * @see <a href="https://github.com/Knotx/knotx-data-bridge">Knot.x Data Bridge</a>
 */
@Deprecated
public class FragmentProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(FragmentProcessor.class);

  private final ServiceEngine serviceEngine;

  public FragmentProcessor(Vertx vertx, ServiceKnotOptions options) {
    this.serviceEngine = new ServiceEngine(vertx, options);
  }

  public Single<FragmentContext> processSnippet(final FragmentContext fragmentContext,
      KnotContext request) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Processing Handlebars snippet {}", fragmentContext.fragment());
    }
    return Observable.just(fragmentContext)
        .flatMap(FragmentContext::services)
        .map(serviceEngine::mergeWithConfiguration)
        .doOnNext(this::traceService)
        .flatMap(serviceEntry ->
            fetchServiceData(serviceEntry, request).toObservable()
                .map(serviceEntry::getResultWithNamespaceAsKey))
        .reduce(new JsonObject(), JsonObject::mergeIn)
        .map(results -> applyData(fragmentContext, results))
        .onErrorReturn(e -> {
          LOGGER.error("Fragment processing failed. Cause:%s\nRequest:\n%s\nFragmentContext:\n%s\n %s", e.getMessage(), request.getClientRequest(), fragmentContext);
          FragmentUtil.failure(fragmentContext.fragment(), ServiceKnotProxyImpl.SUPPORTED_FRAGMENT_ID, e);
          if (fragmentContext.fragment().hasFallback()) {
            return fragmentContext;
          } else {
            if (e instanceof RuntimeException) {
              throw (RuntimeException) e;
            } else {
              throw new ExecutionException(e);
            }
          }
        });
  }

  private Single<JsonObject> fetchServiceData(ServiceEntry service, KnotContext request) {
    LOGGER.debug("Fetching data from service {} {}", service.getAddress(), service.getParams());
    try {
      return request.getCache()
          .get(service.getCacheKey(), () -> {
            LOGGER.debug("Requesting data from adapter {} with params {}", service.getAddress(),
                service.getParams());
            return serviceEngine.doServiceCall(service, request).cache();
          });
    } catch (ExecutionException e) {
      LOGGER.fatal("Unable to get service data {}", e);
      return Single.error(e);
    }
  }

  private FragmentContext applyData(final FragmentContext fragmentContext,
      JsonObject serviceResult) {
    LOGGER.trace("Applying data to snippet {}", fragmentContext);
    fragmentContext.fragment().context().mergeIn(serviceResult);
    FragmentUtil.success(fragmentContext.fragment(), ServiceKnotProxyImpl.SUPPORTED_FRAGMENT_ID);
    return fragmentContext;
  }

  private void traceService(ServiceEntry serviceEntry) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Found service call definition: {} {}", serviceEntry.getAddress(),
          serviceEntry.getParams());
    }
  }

}
