/*
 * Copyright 2014-2016 Squarespace, Inc.
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

package com.squarespace.jersey2.guice;

import com.google.inject.Injector;
import com.google.inject.servlet.RequestScoped;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.glassfish.hk2.api.ServiceLocator;

public class JerseyGuiceModule extends JerseyModule {

  private final ServiceLocator locator;
  private final boolean useGuiceServlet;
  
  public JerseyGuiceModule(String name) {
    this(JerseyGuiceUtils.newServiceLocator(name));
  }

  public JerseyGuiceModule(String name, boolean useGuiceServlet) {
    this(JerseyGuiceUtils.newServiceLocator(name), useGuiceServlet);
  }
  
  public JerseyGuiceModule(ServiceLocator locator) {
    this(locator, true);
  }

  public JerseyGuiceModule(ServiceLocator locator, boolean useGuiceServlet) {
    this.locator = locator;
    this.useGuiceServlet = useGuiceServlet;
  }

  @Override
  protected void configure() {
    
    Provider<Injector> injector = getProvider(Injector.class);
    bind(ServiceLocator.class).toProvider(new ServiceLocatorProvider(injector, locator))
      .in(Singleton.class);

    if (useGuiceServlet) {
      Provider<ServiceLocator> provider = getProvider(ServiceLocator.class);

      bind(Application.class)
        .toProvider(new JerseyProvider<>(provider, Application.class));

      bind(Providers.class)
        .toProvider(new JerseyProvider<>(provider, Providers.class));

      bind(UriInfo.class)
        .toProvider(new JerseyProvider<>(provider, UriInfo.class))
        .in(RequestScoped.class);

      bind(HttpHeaders.class)
        .toProvider(new JerseyProvider<>(provider, HttpHeaders.class))
        .in(RequestScoped.class);

      bind(SecurityContext.class)
        .toProvider(new JerseyProvider<>(provider, SecurityContext.class))
        .in(RequestScoped.class);

      bind(Request.class)
        .toProvider(new JerseyProvider<>(provider, Request.class))
        .in(RequestScoped.class);
    }
  }
  
  private static class JerseyProvider<T> implements Provider<T> {
    
    private Provider<ServiceLocator> provider;
    
    private final Class<T> type;
    
    public JerseyProvider(Provider<ServiceLocator> provider, Class<T> type) {
      this.provider = provider;
      this.type = type;
    }
    
    @Override
    public T get() {
      ServiceLocator locator = provider.get();
      return locator.getService(type);
    }
  }
  
  private static class ServiceLocatorProvider implements Provider<ServiceLocator> {
    
    private final Provider<Injector> provider;
    
    private final ServiceLocator locator;
    
    @Inject
    public ServiceLocatorProvider(Provider<Injector> provider, ServiceLocator locator) {
      this.provider = provider;
      this.locator = locator;
    }
    
    @Override
    public ServiceLocator get() {
      Injector injector = provider.get();
      JerseyGuiceUtils.link(locator, injector);
      
      return locator;
    }
  }
}
