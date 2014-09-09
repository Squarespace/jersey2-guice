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
import static com.squarespace.jersey2.guice.BindingUtils.newThreeThirtyInjectionResolverDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;
import org.jvnet.hk2.internal.DefaultClassAnalyzer;
import org.jvnet.hk2.internal.DynamicConfigurationImpl;
import org.jvnet.hk2.internal.DynamicConfigurationServiceImpl;
import org.jvnet.hk2.internal.ServiceLocatorImpl;

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
  
  private static final String PREFIX = "GuiceServiceLocator-";
  
  private static final AtomicInteger NTH = new AtomicInteger();
  
  private BootstrapUtils() {}
  
  /**
   * Restores Jersey's default state.
   * 
   * @see ServiceLocatorGeneratorImpl
   * @see RuntimeDelegate#setInstance(RuntimeDelegate)
   */
  public static void reset() {
    InjectionsUtils.install(new ServiceLocatorGeneratorImpl());
    RuntimeDelegate.setInstance(null);
  }
  
  /**
   * @see #install(ServiceLocatorGenerator, ServiceLocator)
   */
  public static void install(ServiceLocator locator) {
    install(new GuiceServiceLocatorGenerator(locator), locator);
  }
  
  /**
   * Installs the given {@link GuiceServiceLocatorGenerator} and {@link ServiceLocator} using reflection.
   *
   * @see InjectionsUtils#install(org.glassfish.hk2.extension.ServiceLocatorGenerator)
   * @see RuntimeDelegate#setInstance(RuntimeDelegate)
   */
  public static void install(ServiceLocatorGenerator generator, ServiceLocator locator) {
    
    InjectionsUtils.install(generator);
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
      = newThreeThirtyInjectionResolverDescriptor(locator);
    
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
   * @see #newInjector(ServiceLocator, Stage, Iterable)
   */
  public static Injector newInjector(ServiceLocator locator, Iterable<? extends Module> modules) {
    return newInjector(locator, null, modules);
  }
  
  /**
   * Creates and returns a {@link Injector}.
   * 
   * @see #newServiceLocator(String, ServiceLocator)
   * @see #link(ServiceLocator, Injector)
   * @see BootstrapModule
   */
  public static Injector newInjector(ServiceLocator locator, Stage stage, Iterable<? extends Module> modules) {
    
    List<Module> copy = new ArrayList<>();
    for (Module module : modules) {
      copy.add(module);
    }
    
    copy.add(new BootstrapModule(locator));
    
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
    
    link(locator, child, Collections.<GuiceBinding<?>>emptySet());
    
    return child;
  }
  
  /**
   * This method links the {@link Injector} to the {@link ServiceLocator}.
   * 
   * @see #newInjector(ServiceLocator, Iterable)
   * @see #newInjector(ServiceLocator, Stage, Iterable)
   */
  public static void link(ServiceLocator locator, Injector injector) {
    Set<GuiceBinding<?>> bindings = injector.getInstance(GuiceBinding.KEY);
    link(locator, injector, bindings);
  }
  
  /**
   * @see #link(ServiceLocator, Injector)
   * @see #newChildInjector(Injector, ServiceLocator)
   */
  private static void link(ServiceLocator locator, Injector injector, Set<? extends GuiceBinding<?>> bindings) {
    DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
    DynamicConfiguration dc = dcs.createDynamicConfiguration();
    
    GuiceJustInTimeResolver resolver = new GuiceJustInTimeResolver(locator, injector);
    dc.bind(BuilderHelper.createConstantDescriptor(resolver));
    
    dc.addActiveDescriptor(GuiceScopeContext.class);
    
    bind(locator, dc, new MessagingBinders.HeaderDelegateProviders());
    
    for (GuiceBinding<?> binding : bindings) {
      bind(locator, dc, binding.newBinder(injector));
    }
    
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
  private static Injector createInjector(Stage stage, Iterable<? extends Module> modules) {
    if (stage != null) {
      return Guice.createInjector(stage, modules);
    }
    
    return Guice.createInjector(modules);
  }
}
