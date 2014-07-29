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

import static com.squarespace.jersey2.guice.BindingUtils.newGuiceInjectionResolverDescriptor;
import static com.squarespace.jersey2.guice.BindingUtils.newServiceLocatorDescriptor;
import static com.squarespace.jersey2.guice.BindingUtils.newThreeTirtyInjectionResolverDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.jvnet.hk2.internal.DefaultClassAnalyzer;
import org.jvnet.hk2.internal.DynamicConfigurationImpl;
import org.jvnet.hk2.internal.DynamicConfigurationServiceImpl;
import org.jvnet.hk2.internal.ServiceLocatorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

/**
 * An utility class to bootstrap HK2's {@link ServiceLocator}s and {@link Guice}'s {@link Injector}s.
 * 
 * @see ServiceLocator
 * @see Injector
 */
public class BootstrapUtils {

  private static final Logger LOG = LoggerFactory.getLogger(BootstrapUtils.class);
  
  private static final String PREFIX = "GuiceServiceLocator-";
  
  private static final AtomicInteger NTH = new AtomicInteger();
  
  private BootstrapUtils() {}
  
  /**
   * @see #install(ServiceLocator, boolean)
   */
  public static void install(ServiceLocator locator) {
    install(locator, false);
  }
  
  /**
   * This is a convenience method to make {@link ServiceLocator} installation easier.
   * 
   * NOTE: It's being assumed that the given {@link ServiceLocator} is fully wired for {@link Guice}.
   * 
   * @see InjectionsUtils#setServiceLocatorGenerator(ServiceLocatorGenerator)
   * @see InjectionsUtils#setServiceLocatorFactory(ServiceLocatorFactory)
   * @see RuntimeDelegate#setInstance(RuntimeDelegate)
   */
  public static void install(ServiceLocator locator, boolean useReflection) {
    ServiceLocatorGenerator generator = new GuiceServiceLocatorGenerator(locator);
    
    // Don't use reflection if the issue is fixed.
    if (!InjectionsUtils.hasFix() || useReflection) {
      
      if (LOG.isInfoEnabled()) {
        LOG.info("Using reflection to install ServiceLocatorGenerator: {}", generator);
      }
      
      InjectionsUtils.setServiceLocatorGenerator(generator);
      InjectionsUtils.setServiceLocatorFactory(new GuiceServiceLocatorFactory(generator));
    
    } else {
      
      if (LOG.isInfoEnabled()) {
        LOG.info("Using SPI to install ServiceLocatorGenerator: {}", generator);
      }
      
      InjectionsUtils.installGeneratorSPI(generator);
    }
    
    RuntimeDelegate.setInstance(new GuiceRuntimeDelegate(locator));
  }
  
  /**
   * @see #newServiceLocator(String, ServiceLocator)
   */
  public static ServiceLocator newServiceLocator() {
    return newServiceLocator(null);
  }
  
  /**
   * @see #newServiceLocator(String, ServiceLocator)
   */
  public static ServiceLocator newServiceLocator(String name) {
    return newServiceLocator(name, null);
  }
  
  /**
   * Creates and returns a {@link ServiceLocator}.
   */
  public static ServiceLocator newServiceLocator(String name, ServiceLocator parent) {
    if (parent != null && !(parent instanceof ServiceLocatorImpl)) {
      throw new IllegalArgumentException("name=" + name + ", parent=" + parent);
    }
    
    if (name == null) {
      name = PREFIX;
    }
    
    if (name.endsWith("-")) {
      name += NTH.incrementAndGet();
    }
    
    GuiceServiceLocator locator = new GuiceServiceLocator(name, parent);
    
    DynamicConfigurationImpl config = new DynamicConfigurationImpl(locator);
    
    config.bind(newServiceLocatorDescriptor(locator));
    
    ActiveDescriptor<InjectionResolver<javax.inject.Inject>> threeThirtyResolver 
      = newThreeTirtyInjectionResolverDescriptor(locator);
    
    config.addActiveDescriptor(threeThirtyResolver);
    config.addActiveDescriptor(newGuiceInjectionResolverDescriptor(
        locator, threeThirtyResolver));
    
    config.bind(BuilderHelper.link(DynamicConfigurationServiceImpl.class, false).
            to(DynamicConfigurationService.class).
            in(Singleton.class.getName()).
            localOnly().
            build());
    
    config.bind(BuilderHelper.createConstantDescriptor(
            new DefaultClassAnalyzer(locator)));
    
    config.commit();
    return locator;
  }
  
  /**
   * @see #newInjector(ServiceLocator, Stage, List)
   */
  public static Injector newInjector(ServiceLocator locator, List<? extends Module> modules) {
    return newInjector(locator, null, modules);
  }
  
  /**
   * Creates and returns a {@link Injector}.
   * 
   * @see #newServiceLocator(String, ServiceLocator)
   */
  public static Injector newInjector(ServiceLocator locator, Stage stage, List<? extends Module> modules) {
    
    List<Module> copy = new ArrayList<>(modules);
    copy.add(new JerseyToGuiceModule(locator));
    
    Injector injector = createInjector(stage, copy);
    
    link(locator, injector);
    
    return injector;
  }
  
  /**
   * Creates and returns a child {@link Injector}.
   * 
   * @see Injector#createChildInjector(Module...)
   * @see Injector#createChildInjector(Iterable)
   */
  public static Injector newChildInjector(Injector injector, ServiceLocator locator) {
    Injector child = injector.createChildInjector(new ServiceLocatorModule(locator));
    
    link(locator, child);
    
    return child;
  }
  
  /**
   * This method links the {@link Injector} to the {@link ServiceLocator}.
   */
  private static void link(ServiceLocator locator, Injector injector) {
    DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
    DynamicConfiguration dc = dcs.createDynamicConfiguration();
    
    GuiceJustInTimeResolver resolver = new GuiceJustInTimeResolver(locator, injector);
    dc.bind(BuilderHelper.createConstantDescriptor(resolver));
    
    dc.addActiveDescriptor(GuiceScopeContext.class);
    
    bind(locator, dc, new MessagingBinders.HeaderDelegateProviders());
    bind(locator, dc, new InjectorBinder(injector));
    
    dc.commit();
  }
  
  /**
   * @see ServiceLocator#inject(Object)
   * @see Binder#bind(DynamicConfiguration)
   */
  private static void bind(ServiceLocator locator, DynamicConfiguration dc, Binder binder) {
    locator.inject(binder);
    binder.bind(dc);
  }
  
  /**
   * This method takes care of a {@code null} {@link Stage} argument.
   * 
   * @see Guice#createInjector(Iterable)
   * @see Guice#createInjector(Stage, Iterable)
   */
  private static Injector createInjector(Stage stage, List<? extends Module> modules) {
    if (stage != null) {
      return Guice.createInjector(stage, modules);
    }
    
    return Guice.createInjector(modules);
  }
  
  /**
   * This is a HK2 {@link Binder} for the {@link Guice} {@link Injector}.
   * It makes the {@link Injector} injectable in the HK2 world.
   */
  private static class InjectorBinder extends AbstractBinder {
    
    private final Injector injector;

    public InjectorBinder(Injector injector) {
      this.injector = injector;
    }

    @Override
    protected void configure() {
      bind(injector).to(Injector.class);
    }
  }
}
