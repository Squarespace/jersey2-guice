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

import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.api.ServiceLocatorListener;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.internal.ServiceLocatorFactoryImpl;

/**
 * This is an alternative to using the {@link GuiceServiceLocatorGeneratorStub} SPI.
 * 
 * The idea is two swap out the {@link ServiceLocatorFactory#getInstance()} value 
 * and then pass our own {@link ServiceLocatorGenerator} into the {@code create(...)}
 * methods instead of letting {@link ServiceLocatorFactoryImpl} pick one via SPI.
 */
class GuiceServiceLocatorFactory extends ServiceLocatorFactory {

  private final AtomicReference<ServiceLocatorGenerator> generatorRef 
      = new AtomicReference<>();
  
  private final ServiceLocatorFactory factory;

  public GuiceServiceLocatorFactory(ServiceLocatorFactory factory) {
    this.factory = factory;
  }
  
  public ServiceLocatorGenerator install(ServiceLocatorGenerator generator) {
    return generatorRef.getAndSet(generator);
  }
  
  public ServiceLocatorGenerator get() {
    return generatorRef.get();
  }

  @Override
  public ServiceLocator create(String name) {
    return create(name, null);
  }

  @Override
  public ServiceLocator create(String name, ServiceLocator parent) {
    return create(name, parent, null);
  }

  @Override
  public ServiceLocator create(String name, ServiceLocator parent, 
      ServiceLocatorGenerator generator) {
    return create(name, parent, generator, CreatePolicy.RETURN);
  }

  @Override
  public ServiceLocator create(String name, ServiceLocator parent, 
      ServiceLocatorGenerator generator,
      CreatePolicy policy) {
    
    // NOTE: A non-null generator would be a bit unexpected here. It'd mean
    // that something else is attempting to pass one in and it's no longer 
    // clear what's supposed to happen. We're going to respect it but it's 
    // most likely wrong. But speaking strictly in terms of Jersey please see
    // Injections#_createLocator(...) which calls this method with a null arg.
    
    if (generator == null) {
      generator = generatorRef.get();
    }
    
    if (generator == null) {
      throw new IllegalStateException("There is no ServiceLocatorGenerator installed.");
    }
    
    return factory.create(name, parent, generator, policy);
  }

  @Override
  public ServiceLocator find(String name) {
    return factory.find(name);
  }

  @Override
  public void destroy(String name) {
    factory.destroy(name);
  }

  @Override
  public void destroy(ServiceLocator locator) {
    factory.destroy(locator);
  }

  @Override
  public void addListener(ServiceLocatorListener listener) {
    factory.addListener(listener);
  }

  @Override
  public void removeListener(ServiceLocatorListener listener) {
    factory.removeListener(listener);
  }
}
