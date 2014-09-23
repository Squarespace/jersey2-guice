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

import java.util.List;
import java.util.ServiceLoader;

import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

/**
 * To use SPI extend this class and make an entry for it in the {@code META-INF/services/org.glassfish.hk2.extension.ServiceLocatorGenerator}
 * file. See {@link ServiceLoader} for more information.
 * 
 * NOTE: You need Jersey 2.11+ and this is incompatible to {@link JerseyGuiceServletContextListener}.
 * 
 * <p>{@code META-INF/services/org.glassfish.hk2.extension.ServiceLocatorGenerator}
 * 
 * @see ServiceLoader
 * @see JerseyGuiceServletContextListener
 */
public abstract class JerseyServiceLocatorGeneratorSPI implements ServiceLocatorGenerator {
  
  protected final ServiceLocator locator;
  
  protected final Injector injector;
  
  private final ServiceLocatorGenerator generator;
  
  public JerseyServiceLocatorGeneratorSPI() {
    
    Stage stage = stage();
    List<? extends Module> modules = modules();
    
    this.locator = BootstrapUtils.newServiceLocator();
    this.injector = BootstrapUtils.newInjector(locator, stage, modules);
    
    this.generator = new GuiceServiceLocatorGeneratorImpl(locator);
    RuntimeDelegate.setInstance(new GuiceRuntimeDelegate(locator));
  }
  
  /**
   * Returns the {@link ServiceLocator}.
   */
  public ServiceLocator getServiceLocator() {
    return locator;
  }
  
  /**
   * Returns the {@link Injector}.
   */
  public Injector getInjector() {
    return injector;
  }
  
  /**
   * Returns the {@link Stage} to use.
   */
  protected Stage stage() {
    return null;
  }
  
  /**
   * Returns a {@link List} of {@link Module}s.
   */
  protected abstract List<? extends Module> modules();
  
  @Override
  public ServiceLocator create(String name, ServiceLocator parent) {
    return generator.create(name, parent);
  }
}
