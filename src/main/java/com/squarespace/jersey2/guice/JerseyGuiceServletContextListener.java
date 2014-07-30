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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;

/**
 * Extend this class and make the same entries as for {@link GuiceServletContextListener}
 * in the {@literal web.xml} file.
 * 
 * NOTE: This is incompatible to {@link JerseyServiceLocatorGeneratorSPI}.
 * 
 * @see JerseyServiceLocatorGeneratorSPI
 * @see GuiceServletContextListener
 */
public abstract class JerseyGuiceServletContextListener implements ServletContextListener {
  
  protected final ServiceLocator locator;
  
  protected final Injector injector;
  
  private final GuiceServiceLocatorGenerator generator;
  
  public JerseyGuiceServletContextListener() {
    
    Stage stage = stage();
    List<? extends Module> modules = modules();
    
    this.locator = BootstrapUtils.newServiceLocator();
    this.injector = BootstrapUtils.newInjector(locator, stage, modules);
    this.generator = new GuiceServiceLocatorGenerator(locator);
    
    BootstrapUtils.install(generator, locator);
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
  public void contextInitialized(ServletContextEvent sce) {
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }
}
