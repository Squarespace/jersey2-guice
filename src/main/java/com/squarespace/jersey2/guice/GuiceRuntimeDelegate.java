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

import javax.ws.rs.core.Application;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.AbstractRuntimeDelegate;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.internal.RuntimeDelegateImpl;

/**
 * This class replicates Jersey's own {@link RuntimeDelegateImpl} class with 
 * the difference that we're passing in a {@link ServiceLocator} instead of
 * creating a new one.
 * 
 * @see RuntimeDelegateImpl
 */
class GuiceRuntimeDelegate extends AbstractRuntimeDelegate {
  
  public GuiceRuntimeDelegate(ServiceLocator locator) {
    super(locator);
  }
  
  /**
   * @see RuntimeDelegateImpl#createEndpoint(Application, Class)
   */
  @Override
  public <T> T createEndpoint(Application application, Class<T> endpointType) 
      throws IllegalArgumentException, UnsupportedOperationException {
    
    if (application == null) {
      throw new IllegalArgumentException("application is null.");
    }
    
    return ContainerFactory.createContainer(endpointType, application);
  }
}
