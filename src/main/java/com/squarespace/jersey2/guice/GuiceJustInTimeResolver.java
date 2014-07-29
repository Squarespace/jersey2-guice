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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DescriptorType;
import org.glassfish.hk2.api.DescriptorVisibility;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.JustInTimeInjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.reflection.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

/**
 * A {@link JustInTimeInjectionResolver} that is backed by {@link Guice}.
 */
class GuiceJustInTimeResolver implements JustInTimeInjectionResolver {

  private static final Logger LOG = LoggerFactory.getLogger(GuiceJustInTimeResolver.class);
  
  private final ServiceLocator locator;
  
  private final Injector injector;
  
  public GuiceJustInTimeResolver(ServiceLocator locator, Injector injector) {
    this.locator = locator;
    this.injector = injector;
  }

  @Override
  public boolean justInTimeResolution(Injectee injectee) {
    Type type = injectee.getRequiredType();
    Class<?> clazz = getClass(type);
    
    if (clazz != null) {
      Binding<?> binding = findBinding(injectee);
      if (binding != null) {
        Key<?> key = binding.getKey();
        Set<Annotation> qualifiers = BindingUtils.getQualifiers(key, true);
        
        GuiceBindingDescriptor<?> descriptor = new GuiceBindingDescriptor<>(
            type, clazz, qualifiers, binding);
        ServiceLocatorUtilities.addOneDescriptor(locator, descriptor);
        return true;
      }
    }
    
    
    return false;
  }
  
  /**
   * Returns a {@link Guice} {@link Binding} for the given HK2 {@link Injectee}
   * or {@code null} if there is no such binding (i.e. Guice doesn't have it and
   * doesn't know how to build it).
   */
  private Binding<?> findBinding(Injectee injectee) {
    Key<?> key = BindingUtils.toKey(injectee);
    if (key != null) {
      // We've to use Injector#getBinding() to cover Just-In-Time bindings
      // which may fail with an Exception because Guice doesn't know how to
      // construct the requested object.
      try {
        return injector.getBinding(key);
      } catch (Exception err) {
        LOG.error("Exception: injectee={}, key={}", injectee, key, err);
      }
    }
    
    return null;
  }
  
  /**
   * Returns a {@link Class} for the given {@link Type} or {@code null}.
   */
  private static Class<?> getClass(Type type) {
    
    if (type instanceof Class<?>) {
      return (Class<?>)type;
    }
    
    if (type instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType)type;
      return (Class<?>)pt.getRawType();
    }
    
    return null;
  }
  
  /**
   * An {@link ActiveDescriptor} that is backed by a {@link Guice} {@link Binding}.
   */
  private static class GuiceBindingDescriptor<T> extends AbstractActiveDescriptor<T> {

    private final Class<?> clazz;
    
    private final Binding<T> binding;
    
    public GuiceBindingDescriptor(Type type, Class<?> clazz, Set<Annotation> qualifiers, Binding<T> binding) {
      super(Collections.singleton(type), GuiceScope.class, 
          ReflectionHelper.getNameFromAllQualifiers(qualifiers, clazz), 
          qualifiers, DescriptorType.CLASS, DescriptorVisibility.NORMAL,
          0, false, (Boolean)null, (String)null,
          Collections.<String, List<String>>emptyMap());
      
      this.clazz = clazz;
      this.binding = binding;
      
      setImplementation(clazz.getName());
    }

    @Override
    public Class<?> getImplementationClass() {
      return clazz;
    }

    @Override
    public T create(ServiceHandle<?> root) {
      return binding.getProvider().get();
    }
  }
}
