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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.hk2.api.ServiceLocator;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.ServletModule;

public class SingletonResourceTest {
  
  @AfterTest
  public void reset() {
    BootstrapUtils.reset();
  }
  
  @Test
  public void singletons() throws IOException {
    ServiceLocator locator = BootstrapUtils.newServiceLocator();
    
    List<Module> modules = new ArrayList<>();
    modules.add(new ServletModule());
    
    @SuppressWarnings("unused")
    Injector injector = BootstrapUtils.newInjector(locator, modules);
    
    BootstrapUtils.install(locator);
    
    try (HttpServer server = HttpServerUtils.newHttpServer(MySingletonResource.class, MyGuiceSingletonResource.class)) {
      check();
    }
  }
  
  private void check() throws IOException {
    
    String url = "http://localhost:" + HttpServerUtils.PORT;
    
    String[] paths = { MySingletonResource.PATH, MyGuiceSingletonResource.PATH };
    
    // Client #1: Create a new Client instance for each request
    for (int i = 0; i < paths.length; i++) {
      
      String expected = null;
      
      for (int j = 0; j < 2; j++) {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget target = client.target(url).path(paths[i]);
            String value = target.request(MediaType.TEXT_PLAIN).get(String.class);
        
            if (j == 0) {
              expected = value;
            } else {
              assertNotNull(expected);
              assertEquals(value, expected, 
                  "value=" + value 
                  + ", expected=" + expected 
                  + ", path=" + paths[i]);
            }
            
        } finally {
          client.close();
        }
      }
    }
  }
}
