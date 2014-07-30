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

import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.client.Client;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.ApplicationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * This class is a bit tricky.
 * 
 * Jersey creates a {@link ServiceLocator} for each {@link ApplicationHandler} and {@link Client} 
 * instance. You're most likely not caring about the existence of {@link ServiceLocator} if you 
 * have a {@link Guice} application but it's something to be aware of.
 * 
 * Due to some post-initialization that is happening in {@link Injections} it's not possible to 
 * share a singleton instance of {@link ServiceLocator}. This class attempts to give the "root" 
 * {@link ServiceLocator} to the first {@link ApplicationHandler} that calls it and subsequent 
 * callers will get their own copy of {@link ServiceLocator} that is still backed by the same 
 * {@link Injector} instance. 
 * 
 * @see ServiceLocatorModule
 */
class GuiceServiceLocatorGenerator implements ServiceLocatorGenerator {
  
  private static final Logger LOG = LoggerFactory.getLogger(GuiceServiceLocatorGenerator.class);
  
  private final AtomicInteger nth = new AtomicInteger();
  
  final ServiceLocator root;
  
  public GuiceServiceLocatorGenerator(ServiceLocator root) {
    this.root = root;
  }
  
  @Override
  public ServiceLocator create(String name, ServiceLocator parent) {
    
    if (isApplicationHandler()) {
      if (nth.incrementAndGet() == 1) {
        return root;
      }
      
      if (LOG.isInfoEnabled()) {
        LOG.info("You have {} ApplicationHandler instance(s)", nth);
      }
    }
    
    Injector injector = root.getService(Injector.class);
    ServiceLocator locator = BootstrapUtils.newServiceLocator(name, parent);
    
    @SuppressWarnings("unused")
    Injector child = BootstrapUtils.newChildInjector(injector, locator);
    
    return locator;
  }
  
  /**
   * Returns {@code true} if the {@link #create(String, ServiceLocator)} call originates
   * from the {@link ApplicationHandler}.
   */
  private static boolean isApplicationHandler() {
    String name = ApplicationHandler.class.getName();
    
    for (StackTraceElement element : stack()) {
      if (element.getClassName().equals(name)) {
        return true;
      }
    }
    
    return false;
  }
  
  private static StackTraceElement[] stack() {
    return (new Exception()).getStackTrace();
  }
}
