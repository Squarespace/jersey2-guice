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
import java.lang.reflect.Type;
import java.util.Set;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * The {@link GuiceBinding} makes {@link Guice} {@link Binding}s available for
 * injection within HK2.
 */
public class GuiceBinding<T> {
  
  /**
   * @see #bind(Binder, Key)
   */
  private static final TypeLiteral<GuiceBinding<?>> TYPE = new TypeLiteral<GuiceBinding<?>>(){};
  
  /**
   * @see #bind(Binder, Key)
   */
  private static final Named NAME = Names.named("GuiceBinding.NAME");
  
  /**
   * The {@link Key} of the {@link Multibinder} {@link Set}.
   * 
   * @see Injector#getInstance(Key)
   */
  public static final Key<Set<GuiceBinding<?>>> KEY = Key.get(new TypeLiteral<Set<GuiceBinding<?>>>(){}, NAME);
  
  /**
   * @see #bind(Binder, Key)
   */
  public static <T> void bind(Binder binder, Class<T> clazz) {
    bind(binder, Key.get(clazz));
  }
  
  /**
   * @see #bind(Binder, Key)
   */
  public static <T> void bind(Binder binder, TypeLiteral<T> typeLiteral) {
    bind(binder, Key.get(typeLiteral));
  }
  
  /**
   * Adds the given {@link Key} to the {@link #KEY} {@link Multibinder}.
   */
  public static <T> void bind(Binder binder, Key<T> key) {
    Multibinder.newSetBinder(binder, TYPE, NAME)
      .addBinding()
      .toInstance(new GuiceBinding<>(key));
  }
  
  private final Key<T> key;
  
  private GuiceBinding(Key<T> key) {
    this.key = key;
  }
  
  /**
   * Returns the {@link Key}.
   */
  public Key<T> getKey() {
    return key;
  }
  
  /**
   * Creates and returns a HK2 {@link org.glassfish.hk2.utilities.Binder}.
   */
  org.glassfish.hk2.utilities.Binder newBinder(final Injector injector) {
    return new AbstractBinder() {
      @Override
      protected void configure() {
        bind(descriptor(injector));
      }
    };
  }
  
  /**
   * @see #newBinder(Injector)
   */
  private GuiceBindingDescriptor<T> descriptor(Injector injector) {
    
    Binding<T> binding = injector.getBinding(key);
    
    Type type = key.getTypeLiteral().getType();
    Class<?> clazz = key.getTypeLiteral().getRawType();
    
    Set<Annotation> qualifiers = BindingUtils.getQualifiers(key, false);
    
    return new GuiceBindingDescriptor<>(type, clazz, qualifiers, binding);
  }
  
  @Override
  public int hashCode() {
    return key.hashCode();
  }
  
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof GuiceBinding<?>)) {
      return false;
    }
    
    return key.equals(((GuiceBinding<?>)o).getKey());
  }
  
  @Override
  public String toString() {
    return key.toString();
  }
}
