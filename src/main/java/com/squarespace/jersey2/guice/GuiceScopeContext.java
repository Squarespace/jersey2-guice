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

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Context;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.annotations.Service;

@Service
class GuiceScopeContext implements Context<GuiceScope> {

  @Override
  public Class<? extends Annotation> getScope() {
    return GuiceScope.class;
  }

  @Override
  public <U> U findOrCreate(ActiveDescriptor<U> descriptor, ServiceHandle<?> root) {
    return descriptor.create(root);
  }

  @Override
  public boolean containsKey(ActiveDescriptor<?> descriptor) {
    return false;
  }

  @Override
  public void destroyOne(ActiveDescriptor<?> descriptor) {
  }

  @Override
  public boolean supportsNullCreation() {
    return false;
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public void shutdown() {
  }
}
