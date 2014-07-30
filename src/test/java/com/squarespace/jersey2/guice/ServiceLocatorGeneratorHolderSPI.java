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

import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;

/**
 * This class gets initialized via SPI.
 */
public class ServiceLocatorGeneratorHolderSPI implements ServiceLocatorGenerator {
  
  private static final AtomicReference<ServiceLocatorGenerator> GENERATOR_REF = new AtomicReference<>();
  
  public static ServiceLocatorGenerator install(ServiceLocator locator) {
    return install(new GuiceServiceLocatorGenerator(locator));
  }
  
  public static ServiceLocatorGenerator install(ServiceLocatorGenerator generator) {
    return GENERATOR_REF.getAndSet(generator);
  }
  
  @Override
  public ServiceLocator create(String name, ServiceLocator parent) {
    ServiceLocatorGenerator generator = GENERATOR_REF.get();
    
    if (generator == null) {
      throw new IllegalStateException();
    }
    
    return generator.create(name, parent);
  }
}
