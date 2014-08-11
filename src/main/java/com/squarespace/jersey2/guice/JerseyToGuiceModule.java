/*
 * Copyright 2014 Squarespace, Inc.
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

import static com.squarespace.jersey2.guice.BindingUtils.toThreeThirtyNamed;

import java.lang.annotation.Annotation;

import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.servlet.RequestScoped;

/**
 * The {@link JerseyToGuiceModule} provides a "bridge" from HK2 to {@link Guice}
 * and makes the following items available:
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
class JerseyToGuiceModule extends AbstractModule {

  private final ServiceLocator locator;
  
  public JerseyToGuiceModule(ServiceLocator locator) {
    this.locator = locator;
  }
  
  @Override
  protected void configure() {
    //bindScope(GuiceScope.class, Scopes.NO_SCOPE);
    
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
  
  /**
   * Takes the given array of {@link Annotation}s and replaces all
   * {@link com.google.inject.name.Named} with {@link javax.inject.Named}.
   * 
   * We do it because HK2 doesn't know about {@link Guice}'s version of named
   * and because it's not a {@link Qualifier} it's being ignored. There
   * is no way to make HK2 aware of the other {@code Named} annotation and 
   * the only option is to rewrite it.
   */
  private static Annotation[] adjust(Annotation... qualifiers) {
    for (int i = 0; i < qualifiers.length; i++) {
      Annotation qualifier = qualifiers[i];
      if (qualifier instanceof com.google.inject.name.Named) {
        qualifiers[i] = toThreeThirtyNamed((com.google.inject.name.Named)qualifier);
      }
    }
    return qualifiers;
  }
  
  private class JerseyProvider<T> implements Provider<T> {
    
    private final Class<T> type;

    private final String name;
    
    private final Annotation[] qualifiers;
    
    public JerseyProvider(Class<T> type, Annotation... qualifiers) {
      this(type, (String)null, qualifiers);
    }
    
    public JerseyProvider(Class<T> type, String name, Annotation... qualifiers) {
      this.type = type;
      this.name = name;
      this.qualifiers = adjust(qualifiers);
    }
    
    @Override
    public T get() {
      if (name != null) {
        return locator.getService(type, name, qualifiers);
      }
      
      return locator.getService(type, qualifiers);
    }
  }
}