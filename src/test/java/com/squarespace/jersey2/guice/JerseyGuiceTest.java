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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.glassfish.hk2.api.ServiceLocator;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;

public class JerseyGuiceTest {

  public static final String NAME = "JerseyGuiceTest.NAME";
  
  private static final String VALUE = "Hello, World!";
  
  private final MyInterceptor interceptor = new MyInterceptor();
  
  private final ServletModule jerseyModule = new ServletModule() {
    @Override
    protected void configureServlets() {
      serve(MyHttpServlet.PATH).with(MyHttpServlet.class);
      filter(MyFilter.PATH).through(MyFilter.class);
    }
  };
  
  private final AbstractModule customModule = new AbstractModule() {
    @Override
    protected void configure() {
      bind(String.class)
        .annotatedWith(Names.named(NAME))
        .toInstance(VALUE);
    }
  };
  
  private final AbstractModule aopModule = new AbstractModule() {
    @Override
    protected void configure() {
      GuiceBinding.bind(binder(), MyResource.class);
      
      bindInterceptor(Matchers.any(), 
        Matchers.annotatedWith(MyAnnotation.class), 
        interceptor);
    }
  };
  
  // This *MUST* run first. Testing SPIs is not fun!
  @Test(groups = "SPI")
  public void useSPI() throws IOException {
    if (!InjectionsUtils.hasFix()) {
      Assert.fail("This test needs Jersey 2.11+");
    }
    
    embedded(true);
  }
  
  @AfterTest
  public void reset() {
    BootstrapUtils.reset();
  }
  
  @Test(dependsOnGroups = "SPI")
  public void useReflection() throws IOException {
    embedded(false);
  }
  
  @Test(dependsOnGroups = "SPI")
  public void useServletContextListener() throws IOException {
    final AtomicInteger counter = new AtomicInteger();
    
    ServletContextListener listener = new ServletContextListener() {
      @Override
      public void contextInitialized(ServletContextEvent sce) {
        (new JerseyGuiceServletContextListener() {
          @Override
          protected List<? extends Module> modules() {
            counter.incrementAndGet();
            return Arrays.asList(jerseyModule, customModule);
          }
        }).contextInitialized(sce);
      }
      
      @Override
      public void contextDestroyed(ServletContextEvent sce) {
      }
    };
    
    try (HttpServer server = HttpServerUtils.newHttpServer(MyResource.class, listener)) {
      check();
    }
    
    // Make sure it called once and once only
    assertEquals(counter.get(), 1);
  }
  
  @Test(dependsOnGroups = "SPI")
  public void checkAOP() throws IOException {
    interceptor.counter.set(0);
    
    embedded(false, aopModule);
    
    assertEquals(interceptor.counter.get(), 2);
  }
  
  private void embedded(boolean useSPI, Module... extras) throws IOException {
    
    ServiceLocator locator = BootstrapUtils.newServiceLocator();
    
    List<Module> modules = new ArrayList<>();
    modules.addAll(Arrays.asList(jerseyModule, customModule));
    modules.addAll(Arrays.asList(extras));
    
    @SuppressWarnings("unused")
    Injector injector = BootstrapUtils.newInjector(locator, modules);
    
    if (useSPI) {
      ServiceLocatorGeneratorHolderSPI.install(locator);
    } else {
      BootstrapUtils.install(locator);
    }
    
    try (HttpServer server = HttpServerUtils.newHttpServer(MyResource.class)) {
      check();
    }
  }
  
  private void check() throws IOException {
    
    String url = "http://localhost:" + HttpServerUtils.PORT;
    
    String[] paths = { MyResource.PATH, MyFilter.PATH, MyHttpServlet.PATH };
    String[] responses = { MyResource.RESPONSE, MyFilter.RESPONSE, MyHttpServlet.RESPONSE };
    
    assertEquals(paths.length, responses.length);
    
    // Client #1: Create a new Client instance for each request
    for (int i = 0; i < paths.length; i++) {
      Client client = ClientBuilder.newClient();
      try {
        
          WebTarget target = client.target(url).path(paths[i]);
          
          String value = target.request(MediaType.TEXT_PLAIN).get(String.class);
          assertEquals(value, String.format(responses[i], VALUE));
      } finally {
        client.close();
      }
    }
    
    // Client #2: Re-Use the same Client instance for each request
    Client client = ClientBuilder.newClient();
    try {
      for (int i = 0; i < paths.length; i++) {
        WebTarget target = client.target(url).path(paths[i]);
        
        String value = target.request(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals(value, String.format(responses[i], VALUE));
      }
    } finally {
      client.close();
    }
  }
  
  @Singleton
  static class MyHttpServlet extends HttpServlet {

    static final String PATH = "/servlet";
    
    public static final String RESPONSE = "MyHttpServlet.RESPONSE: %s";
    
    @Inject
    @Named(NAME)
    private String value;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      
      resp.setContentType(MediaType.TEXT_PLAIN);
      resp.getWriter().write(String.format(RESPONSE, value));
    }
  }
  
  @Singleton
  static class MyFilter implements Filter {

    static final String PATH = "/filter";
    
    public static final String RESPONSE = "MyFilter.RESPONSE: %s";
    
    @Inject
    @Named(NAME)
    private String value;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {
      
      response.setContentType(MediaType.TEXT_PLAIN);
      response.getWriter().write(String.format(RESPONSE, value));
    }

    @Override
    public void destroy() {
    }
  }
  
  private class MyInterceptor implements MethodInterceptor {

    public final AtomicInteger counter = new AtomicInteger();
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      counter.incrementAndGet();
      return invocation.proceed();
    }
  }
}
