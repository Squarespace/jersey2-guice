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

import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * A HK2 {@link Binder} for {@link Guice} type(s).
 * 
 * @see GuiceBindingDescriptor
 */
class GuiceBinder<T> extends AbstractBinder {

  private final Key<T> key;
  
  private final Binding<T> binding;

  public GuiceBinder(Key<T> key, Binding<T> binding) {
    this.key = key;
    this.binding = binding;
  }

  @Override
  protected void configure() {
    bind(descriptor(key, binding));
  }
  
  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof GuiceBinder<?>)) {
      return false;
    }
    
    GuiceBinder<?> other = (GuiceBinder<?>)o;
    return key.equals(other.key);
  }

  @Override
  public String toString() {
    return key.toString();
  }
  
  private static <T> GuiceBindingDescriptor<T> descriptor(Key<T> key, Binding<T> binding) {
    
    TypeLiteral<T> typeLiteral = key.getTypeLiteral();
    
    Type type = typeLiteral.getType();
    Class<?> clazz = typeLiteral.getRawType();
    
    Set<Annotation> qualifiers = BindingUtils.getQualifiers(key, false);
    
    return new GuiceBindingDescriptor<>(type, clazz, qualifiers, binding);
  }
}
