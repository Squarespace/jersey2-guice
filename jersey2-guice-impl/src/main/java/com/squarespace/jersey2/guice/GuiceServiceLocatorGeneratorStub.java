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
import org.glassfish.hk2.extension.ServiceLocatorGenerator;

/**
 * Jersey/HK2 uses unfortunately SPIs for custom {@link ServiceLocator}s.
 * 
 * @see ServiceLocatorGenerator
 */
public class GuiceServiceLocatorGeneratorStub implements ServiceLocatorGenerator {
  
  private static final AtomicReference<ServiceLocatorGenerator> GENERATOR_REF = new AtomicReference<>();
  
  static ServiceLocatorGenerator install(ServiceLocatorGenerator generator) {
    if (generator instanceof GuiceServiceLocatorGeneratorStub) {
      throw new IllegalArgumentException();
    }
    
    return GENERATOR_REF.getAndSet(generator);
  }
  
  static ServiceLocatorGenerator get() {
    return GENERATOR_REF.get();
  }
  
  @Override
  public ServiceLocator create(String name, ServiceLocator parent) {
    ServiceLocatorGenerator generator = GENERATOR_REF.get();
    
    if (generator == null) {
      generator = GENERATOR_REF.getAndSet( new GuiceServiceLocatorGenerator() );
    }
    
    return generator.create(name, parent);
  }
}
