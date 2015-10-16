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

import static com.squarespace.jersey2.guice.BindingUtils.isNullable;

import javax.annotation.Nullable;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.UnsatisfiedDependencyException;
import org.jvnet.hk2.internal.ThreeThirtyResolver;

import com.google.inject.Guice;

/**
 * This is a replacement for HK2's {@link ThreeThirtyResolver}. It adds  support for JSR-305's 
 * {@link Nullable} and {@link Guice}'s own {@link com.google.inject.Inject#optional()}.
 * 
 * @see ThreeThirtyResolver
 * @see BindingUtils#isNullable(Injectee)
 * @see Nullable
 * @see com.google.inject.Inject#optional()
 */
class GuiceThreeThirtyResolver extends AbstractInjectionResolver<javax.inject.Inject> {
  
  private final ServiceLocator locator;
  
  public GuiceThreeThirtyResolver(ServiceLocator locator) {
    this.locator = locator;
  }
  
  @Override
  public Object resolve(Injectee injectee, ServiceHandle<?> root) {
    ActiveDescriptor<?> descriptor = locator.getInjecteeDescriptor(injectee);
    
    if (descriptor == null) {
      
      // Is it OK to return null?
      if (isNullable(injectee)) {
        return null;
      }
      
      throw new MultiException(new UnsatisfiedDependencyException(injectee));
    }
    
    return locator.getService(descriptor, root, injectee);
  }
}
