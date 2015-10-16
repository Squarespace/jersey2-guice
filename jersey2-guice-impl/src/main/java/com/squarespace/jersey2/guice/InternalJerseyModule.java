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

import javax.inject.Provider;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.inject.Guice;
import com.google.inject.servlet.RequestScoped;

/**
 * The {@link InternalJerseyModule} provides a "bridge" from HK2 to {@link Guice}
 * and makes the following items available for injection:
 * 
 * {@link ServiceLocator}
 * {@link Application}
 * {@link Providers}
 * 
 * {@link UriInfo}
 * {@link HttpHeaders}
 * {@link SecurityContext}
 * {@link Request}
 */
class InternalJerseyModule extends JerseyModule {

  private final ServiceLocator locator;
  
  public InternalJerseyModule(ServiceLocator locator) {
    this.locator = locator;
  }
  
  @Override
  protected void configure() {
    bind(ServiceLocator.class).toInstance(locator);
    
    bind(Application.class)
      .toProvider(new JerseyProvider<>(Application.class));
    
    bind(Providers.class)
      .toProvider(new JerseyProvider<>(Providers.class));
    
    bind(UriInfo.class)
      .toProvider(new JerseyProvider<>(UriInfo.class))
      .in(RequestScoped.class);
    
    bind(HttpHeaders.class)
      .toProvider(new JerseyProvider<>(HttpHeaders.class))
      .in(RequestScoped.class);
    
    bind(SecurityContext.class)
      .toProvider(new JerseyProvider<>(SecurityContext.class))
      .in(RequestScoped.class);
    
    bind(Request.class)
      .toProvider(new JerseyProvider<>(Request.class))
      .in(RequestScoped.class);
  }
  
  private class JerseyProvider<T> implements Provider<T> {
    
    private final Class<T> type;
    
    public JerseyProvider(Class<T> type) {
      this.type = type;
    }
    
    @Override
    public T get() {
      return locator.getService(type);
    }
  }
}