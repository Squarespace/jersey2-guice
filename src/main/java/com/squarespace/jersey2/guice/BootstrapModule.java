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

import org.glassfish.hk2.api.ServiceLocator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * This module provides bindings for HK2's {@link ServiceLocator} and 
 * {@link Guice}'s {@link Injector}.
 * 
 * @see BootstrapUtils#newInjector(ServiceLocator, Iterable)
 * @see BootstrapUtils#newInjector(ServiceLocator, com.google.inject.Stage, Iterable)
 */
public class BootstrapModule extends AbstractModule {
  
  private final ServiceLocator locator;
  
  public BootstrapModule(ServiceLocator locator) {
    this.locator = locator;
  }

  @Override
  protected void configure() {
    // Make the Guice Injector available in HK2
    GuiceBinding.bind(binder(), Injector.class);
    
    // Make some HK2 stuff available in Guice
    install(new JerseyToGuiceModule(locator));
  }
}
