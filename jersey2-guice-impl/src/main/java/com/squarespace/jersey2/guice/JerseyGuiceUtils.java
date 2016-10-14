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

import static com.squarespace.jersey2.guice.BindingUtils.newGuiceInjectionResolverDescriptor;
import static com.squarespace.jersey2.guice.BindingUtils.newThreeThirtyInjectionResolverDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;
import org.jvnet.hk2.internal.DefaultClassAnalyzer;
import org.jvnet.hk2.internal.DynamicConfigurationImpl;
import org.jvnet.hk2.internal.DynamicConfigurationServiceImpl;
import org.jvnet.hk2.internal.InstantiationServiceImpl;
import org.jvnet.hk2.internal.ServiceLocatorImpl;
import org.jvnet.hk2.internal.ServiceLocatorRuntimeImpl;
import org.jvnet.hk2.internal.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.spi.ElementSource;

/**
 * An utility class to bootstrap HK2's {@link ServiceLocator}s and {@link Guice}'s {@link Injector}s.
 * 
 * @see ServiceLocator
 * @see Injector
 */
public class JerseyGuiceUtils {
  
  private static final Logger LOG = LoggerFactory.getLogger(JerseyGuiceUtils.class);
  
  private static final String MODIFIERS_FIELD = "modifiers";
  
  private static final String PREFIX = "GuiceServiceLocator-";
  
  private static final AtomicInteger NTH = new AtomicInteger();
  
  private static boolean SPI_CHECKED = false;
  
  private static boolean SPI_PRESENT = false;
  
  private JerseyGuiceUtils() {}
  
  /**
   * Installs the given {@link Injector}.
   * 
   * @see JerseyGuiceModule
   */
  public static void install(Injector injector) {
    // This binding is provided by JerseyGuiceModule
    ServiceLocator locator = injector.getInstance(ServiceLocator.class);
    
    GuiceServiceLocatorGenerator generator = getOrCreateGuiceServiceLocatorGenerator();
    generator.add(locator);
  }
  
  /**
   * Installs a {@link ServiceLocatorGenerator} instead of an {@link Injector}. This 
   * is mostly needed for testing and bootstrapping.
   * 
   * @see #install(Injector)
   */
  public static void install(ServiceLocatorGenerator delegate) {
    GuiceServiceLocatorGenerator generator = getOrCreateGuiceServiceLocatorGenerator();
    generator.delegate(delegate);
  }
  
  /**
   * 
   */
  public static void reset() {
    GuiceServiceLocatorGenerator generator = getOrCreateGuiceServiceLocatorGenerator();
    generator.reset();
  }
  
  private static synchronized GuiceServiceLocatorGenerator getOrCreateGuiceServiceLocatorGenerator() {
    // Use SPI
    if (isProviderPresent()) {
      GuiceServiceLocatorGenerator generator = (GuiceServiceLocatorGenerator)GuiceServiceLocatorGeneratorStub.get();
      if (generator == null) {
        generator = new GuiceServiceLocatorGenerator();
        GuiceServiceLocatorGeneratorStub.install(generator);
      }
      return generator;
    }
    
    // Use Reflection
    GuiceServiceLocatorFactory factory = getOrCreateFactory();
    if (factory != null) {
      GuiceServiceLocatorGenerator generator = (GuiceServiceLocatorGenerator)factory.get();
      if (generator == null) {
        generator = new GuiceServiceLocatorGenerator();
        factory.install(generator);
      }
      
      return generator;
    }
    
    throw new IllegalStateException();
  }
  
  /**
   * Returns {@code true} if jersey2-guice SPI is present.
   */
  private static synchronized boolean isProviderPresent() {
    
    if (!SPI_CHECKED) {
      SPI_CHECKED = true;
      
      ServiceLocatorGenerator generator = lookupSPI();
      if (generator instanceof GuiceServiceLocatorGeneratorStub) {
        SPI_PRESENT = true;
        
      }
      
      if (!SPI_PRESENT) {
        LOG.warn("It appears jersey2-guice-spi is either not present or in conflict with some other Jar: {}", generator);
      }
    }
    
    return SPI_PRESENT;
  }
  
  private static ServiceLocatorGenerator lookupSPI() {
    return AccessController.doPrivileged(new PrivilegedAction<ServiceLocatorGenerator>() {
      @Override
      public ServiceLocatorGenerator run() {
        try {
          ClassLoader classLoader = JerseyGuiceUtils.class.getClassLoader();
          ServiceLoader<ServiceLocatorGenerator> providers 
              = ServiceLoader.load(ServiceLocatorGenerator.class, classLoader);
          
          for (ServiceLocatorGenerator generator : providers) {
            return generator;
          }
        } catch (Throwable th) {
          LOG.warn("Exception", th);
        }
        
        return null;
      }
    });
  }
  
  /**
   * @see ServiceLocatorFactory
   */
  private static synchronized GuiceServiceLocatorFactory getOrCreateFactory() {
    ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
    if (factory instanceof GuiceServiceLocatorFactory) {
      return (GuiceServiceLocatorFactory)factory;
    }
    
    if (LOG.isInfoEnabled()) {
      LOG.info("Attempting to install (using relfection) a Guice-aware ServiceLocatorFactory...");
    }
    
    try {
      GuiceServiceLocatorFactory guiceServiceLocatorFactory 
          = new GuiceServiceLocatorFactory(factory);
      
      Class<?> clazz = ServiceLocatorFactory.class;
      Field field = clazz.getDeclaredField("INSTANCE");
      
      set(field, null, guiceServiceLocatorFactory);

      clazz = Injections.class;
      field = clazz.getDeclaredField("factory");

      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

      field.setAccessible(true);

      set(field, null, guiceServiceLocatorFactory);
      
      return guiceServiceLocatorFactory;
    } catch (Exception err) {err.printStackTrace();
      LOG.error("Exception", err);
    }
    
    return null;
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
   * 
   * NOTE: This code should be very similar to HK2's own {@link ServiceLocatorGeneratorImpl}.
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
    
    config.bind(Utilities.getLocatorDescriptor(locator));
    
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
    
    config.bind(BuilderHelper.createDescriptorFromClass(ServiceLocatorRuntimeImpl.class));
    
    config.bind(BuilderHelper.createConstantDescriptor(
        new InstantiationServiceImpl()));
    
    config.commit();
    return locator;
  }
  
  /**
   * This method links the {@link Injector} to the {@link ServiceLocator}.
   */
  public static ServiceLocator link(ServiceLocator locator, Injector injector) {
    
    Map<Key<?>, Binding<?>> bindings = gatherBindings(injector);
    Set<Binder> binders = toBinders(bindings);
    
    return link(locator, injector, binders);
  }
  
  /**
   * Gathers Guice {@link Injector} bindings over the hierarchy.
   */
  private static Map<Key<?>, Binding<?>> gatherBindings(Injector injector) {
      
    Map<Key<?>, Binding<?>> dst = new HashMap<Key<?>, Binding<?>>();
    
    Injector current = injector;
    while (current != null) {
      dst.putAll(current.getBindings());
      current = current.getParent();
    }
    
    return dst;
  }
  
  /**
   * @see #link(ServiceLocator, Injector)
   * @see #newChildInjector(Injector, ServiceLocator)
   */
  private static ServiceLocator link(ServiceLocator locator, 
      Injector injector, Iterable<? extends Binder> binders) {
    
    DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
    DynamicConfiguration dc = dcs.createDynamicConfiguration();
    
    GuiceJustInTimeResolver resolver = new GuiceJustInTimeResolver(locator, injector);
    dc.bind(BuilderHelper.createConstantDescriptor(resolver));
    
    dc.addActiveDescriptor(GuiceScopeContext.class);
    
    bind(locator, dc, new MessagingBinders.HeaderDelegateProviders());
    
    for (Binder binder : binders) {
      bind(locator, dc, binder);
    }
    
    dc.commit();
    return locator;
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
   * Turns the given Guice {@link Binding}s into HK2 {@link Binder}s.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static Set<Binder> toBinders(Map<Key<?>, Binding<?>> bindings) {
    Set<Binder> binders = new HashSet<>();
    
    for (Map.Entry<Key<?>, Binding<?>> entry : bindings.entrySet()) {
      Key<?> key = entry.getKey();
      Binding<?> binding = entry.getValue();
      
      Object source = binding.getSource();
      if (!(source instanceof ElementSource)) {
        
        // Things like the Injector itself don't have an ElementSource.
        if (LOG.isTraceEnabled()) {
          LOG.trace("Adding binding: key={}, source={}", key, source);
        }
        
        binders.add(new GuiceBinder(key, binding));
        continue;
      }
      
      ElementSource element = (ElementSource)source;
      List<String> names = element.getModuleClassNames();
      String name = names.get(0);
      
      // Skip everything that is declared in a JerseyModule
      try {

        Class<?> module;

        // Attempt to load the classes via the context class loader first, in order to support
        // environments that enforce tighter constraints on class loading (such as in an OSGi container)
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if(classLoader != null) {
          module = classLoader.loadClass(name);
        } else {
          module = Class.forName(name);
        }
        if (JerseyModule.class.isAssignableFrom(module)) {
          if (LOG.isTraceEnabled()) {
            LOG.trace("Ignoring binding {} in {}", key, module);
          }
          
          continue;
        }
      } catch (ClassNotFoundException err) {
        // Some modules may not be able to be instantiated directly here as a class if we're running
        // in a container that enforcer tighter class loader constraints (such as the
        // org.ops4j.peaberry.osgi.OSGiModule Guice module when running in an OSGi container),
        // so we're only logging a warning here instead of throwing a hard exception
        if (LOG.isWarnEnabled()) {
          LOG.warn("Unavailable to load class in order to validate module: name={}", name);
        }
      }
      
      binders.add(new GuiceBinder(key, binding));
    }
    
    return binders;
  }
  
  private static void set(Field field, Object instance, Object value) throws  IllegalAccessException, NoSuchFieldException, SecurityException {
    field.setAccessible(true);
    
    int modifiers = field.getModifiers();
    if (Modifier.isFinal(modifiers)) {
      setModifiers(field, modifiers & ~Modifier.FINAL);
      try {
        field.set(instance, value);
      } finally {
        setModifiers(field, modifiers | Modifier.FINAL);
      }
    } else {
      field.set(instance, value);
    }
  }
  
  private static void setModifiers(Field dst, int modifiers) throws IllegalAccessException, NoSuchFieldException, SecurityException {
    Field field = Field.class.getDeclaredField(MODIFIERS_FIELD);
    field.setAccessible(true);
    field.setInt(dst, modifiers);
  }
}
