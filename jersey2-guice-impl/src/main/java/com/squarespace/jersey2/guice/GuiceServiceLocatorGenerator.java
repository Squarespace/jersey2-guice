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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;

public class GuiceServiceLocatorGenerator implements ServiceLocatorGenerator {
  
  private final ServiceLocatorGenerator generator = new ServiceLocatorGeneratorImpl();
  
  private final ConcurrentMap<String, ServiceLocator> locators = new ConcurrentHashMap<>();
  
  private final AtomicReference<ServiceLocatorGenerator> delegateRef = new AtomicReference<>();
  
  public void delegate(ServiceLocatorGenerator delegate) {
    delegateRef.set(delegate);
  }
  
  public void add(ServiceLocator locator) {
    String name = locator.getName();
    
    if (locators.putIfAbsent(name, locator) != null) {
      throw new IllegalStateException("Duplicate name: " + name);
    }
  }
  
  public Collection<ServiceLocator> locators() {
    return locators.values();
  }
  
  public void reset() {
    locators.clear();
    delegateRef.set(null);
  }
  
  @Override
  public ServiceLocator create(String name, ServiceLocator parent) {
    // Using remove() to transfer ownership of the ServiceLocator from
    // this object to the caller. Something is really wrong if the caller 
    // uses the same name again!
    ServiceLocator locator = locators.remove(name);
    if (locator != null) {
      return locator;
    }
    
    // This is mostly needed for testing.
    ServiceLocatorGenerator delegate = delegateRef.get();
    if (delegate != null) {
      locator = delegate.create(name, parent);
      if (locator != null) {
        return locator;
      }
    }
    
    return generator.create(name, parent);
  }
}