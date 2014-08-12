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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DescriptorType;
import org.glassfish.hk2.api.DescriptorVisibility;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.reflection.ReflectionHelper;

import com.google.inject.Binding;
import com.google.inject.Guice;

/**
 * An {@link ActiveDescriptor} that is backed by a {@link Guice} {@link Binding}.
 */
class GuiceBindingDescriptor<T> extends AbstractActiveDescriptor<T> {
  
  private final Class<?> clazz;
  
  private final Binding<T> binding;
  
  public GuiceBindingDescriptor(Type type, Class<?> clazz, 
      Set<Annotation> qualifiers, Binding<T> binding) {
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