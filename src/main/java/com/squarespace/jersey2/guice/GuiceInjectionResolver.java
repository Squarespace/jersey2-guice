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

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

import com.google.inject.Guice;

/**
 * The {@link GuiceInjectionResolver} delegates all {@link Guice}'s {@link com.google.inject.Inject}
 * binding annotations to JSR-330's {@link javax.inject.Inject}.
 * 
 * @see GuiceThreeThirtyResolver
 */
class GuiceInjectionResolver extends AbstractInjectionResolver<com.google.inject.Inject> {
  
  /**
   * The name of the {@link InjectionResolver} for {@link Guice}'s own
   * {@link com.google.inject.Inject} binding annotation. It's just a
   * delegate to {@link InjectionResolver#SYSTEM_RESOLVER_NAME} and doesn't
   * do anything special.
   */
  public static final String GUICE_RESOLVER_NAME = "GuiceInjectionResolver";
  
  private final ActiveDescriptor<? extends InjectionResolver<?>> descriptor;
  
  public GuiceInjectionResolver(ActiveDescriptor<? extends InjectionResolver<?>> descriptor) {
    this.descriptor = descriptor;
  }
  
  @Override
  public Object resolve(Injectee injectee, ServiceHandle<?> root) {
    InjectionResolver<?> resolver = descriptor.create(root);
    return resolver.resolve(injectee, root);
  }
}
