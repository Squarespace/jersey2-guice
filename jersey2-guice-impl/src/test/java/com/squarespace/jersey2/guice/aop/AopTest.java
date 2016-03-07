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

package com.squarespace.jersey2.guice.aop;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import com.google.inject.servlet.ServletModule;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import com.squarespace.jersey2.guice.JerseyGuiceModule;
import com.squarespace.jersey2.guice.utils.HttpServer;
import com.squarespace.jersey2.guice.utils.HttpServerUtils;

public class AopTest {
  
  @AfterTest
  public void reset() {
    JerseyGuiceUtils.reset();
  }
  
  @Test
  public void checkAOP() throws IOException {
    
    final MyInterceptor interceptor = new MyInterceptor();
    
    final AbstractModule aopModule = new AbstractModule() {
      @Override
      protected void configure() {
        // ATTENTION: This is really important. It binds the 
        // 'MyResource' class to Guice and makes sure that HK2's
        // ServiceLocator will use Guice to instantiate it.
        bind(MyResource.class); 
        
        bindInterceptor(Matchers.any(), 
          Matchers.annotatedWith(MyAnnotation.class), 
          interceptor);
      }
    };
    
    JerseyGuiceUtils.install(new ServiceLocatorGenerator() {
      @Override
      public ServiceLocator create(String name, ServiceLocator parent) {
        if (!name.startsWith("__HK2_")) {
          return null;
        }
        
        List<Module> modules = new ArrayList<>();
        
        modules.add(new JerseyGuiceModule(name));
        modules.add(new ServletModule());
        
        modules.add(aopModule);
        
        return Guice.createInjector(modules)
            .getInstance(ServiceLocator.class);
      }
    });
    
    try (HttpServer server = HttpServerUtils.newHttpServer(MyResource.class)) {
      check();
    }
    
    assertEquals(interceptor.counter.get(), 1);
  }
  
  private void check() throws IOException {
    String url = "http://localhost:" + HttpServerUtils.PORT;
    
    Client client = ClientBuilder.newClient();
    try {
      
        WebTarget target = client.target(url).path(MyResource.PATH);
        
        String value = target.request(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals(value, MyResource.RESPONSE);
    } catch (Exception err) {
      fail("Exception", err);
    } finally {
      client.close();
    }
  }
}
