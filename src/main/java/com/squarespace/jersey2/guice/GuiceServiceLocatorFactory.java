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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.api.ServiceLocatorListener;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.internal.ServiceLocatorFactoryImpl;

/**
 * NOTE: The {@link ServiceLocatorFactory} is not a real factory. It's a 
 * "Get-Or-Create" instance manager and the {@link ServiceLocatorGenerator} 
 * is the actual factory.
 * 
 * @see ServiceLocatorFactoryImpl
 */
class GuiceServiceLocatorFactory extends ServiceLocatorFactory {
  
  private final Object lock = new Object();
  
  private final Map<String, ServiceLocator> locators = new HashMap<>();
  
  private final List<ServiceLocatorListener> listeners = new ArrayList<>();
  
  private final ServiceLocatorGenerator generator;
  
  public GuiceServiceLocatorFactory(ServiceLocator locator) {
    this(new GuiceServiceLocatorGenerator(locator));
  }
  
  private GuiceServiceLocatorFactory(ServiceLocatorGenerator generator) {
    this.generator = generator;
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
    return create(name, parent, null, CreatePolicy.RETURN);
  }

  @Override
  public ServiceLocator create(String name, ServiceLocator parent,
      ServiceLocatorGenerator generator, CreatePolicy policy) {
    
    synchronized (lock) {
      
      ServiceLocator locator = locators.get(name);
      
      // Are we supposed to destroy it?
      if (locator != null && policy == CreatePolicy.DESTROY) {
        destroy(locator);
        locator = null;
      }
      
      if (locator == null) {
        
        if (generator == null) {
          generator = this.generator;
        }
        
        locator = generator.create(name, parent);
        if (locator != null) {
          // Use ServiceLocator#getName() because it may change during construction!
          locators.put(locator.getName(), locator);
          
          for (ServiceLocatorListener listener : listeners) {
            listener.listenerAdded(locator);
          }
        }
      }
      
      return locator;
    }
  }

  @Override
  public ServiceLocator find(String name) {
    synchronized (lock) {
      return locators.get(name);
    }
  }
  
  @Override
  public void destroy(ServiceLocator locator) {
    if (locator != null) {
      destroy(locator.getName());
    }
  }
  
  @Override
  public void destroy(String name) {
    synchronized (lock) {
      ServiceLocator locator = locators.remove(name);
      if (locator != null) {
        locator.shutdown();
        
        for (ServiceLocatorListener listener : listeners) {
          listener.listenerDestroyed(locator);
        }
      }
    }
  }

  @Override
  public void addListener(ServiceLocatorListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("listener");
    }
    
    synchronized (lock) {
      if (listeners.contains(listener)) {
        return;
      }
      
      Set<ServiceLocator> copy = Collections.unmodifiableSet(
          new HashSet<>(locators.values()));
      
      listener.initialize(copy);
      
      listeners.add(listener);
    }
  }

  @Override
  public void removeListener(ServiceLocatorListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("listener");
    }
    
    synchronized (lock) {
      listeners.remove(listener);
    }
  }
  
  public void destroyAll() {
    synchronized (lock) {
      for (ServiceLocator locator : locators.values()) {
        locator.shutdown();
        
        for (ServiceLocatorListener listener : listeners) {
          listener.listenerDestroyed(locator);
        }
      }
      
      locators.clear();
    }
  }
}
