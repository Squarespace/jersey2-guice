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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
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

import org.glassfish.hk2.api.ServiceLocator;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import com.squarespace.jersey2.guice.utils.HttpServer;
import com.squarespace.jersey2.guice.utils.HttpServerUtils;

public class JerseyGuiceTest {

  public static final String NAME = "JerseyGuiceTest.NAME";

  private static final String VALUE = "Hello, World!";

  private static final Class<?>[] RESOURCES = {
    MyResource.class,
    MyAsyncResource.class,
    MyQualifierResource.class
  };

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
      bind(String.class)
        .annotatedWith(MyQualifier.class)
        .toInstance(MyQualifierResource.RESPONSE);
    }
  };
  
  @AfterTest
  public void reset() {
    BootstrapUtils.reset();
  }

  @Test
  public void useReflection() throws IOException {
    BootstrapUtils.install(new GuiceServiceLocatorGenerator() {
      @Override
      protected Injector createInjector(ServiceLocator locator) {
        List<Module> modules = new ArrayList<>();
        
        modules.add(new BootstrapModule(locator));
        modules.add(new ServletModule());
        
        modules.add(jerseyModule);
        modules.add(customModule);
        
        return Guice.createInjector(modules);
      }
    });
    
    try (HttpServer server = HttpServerUtils.newHttpServer(RESOURCES)) {
      check();
    }
  }

  private void check() throws IOException {
    assertTrue(BootstrapUtils.isInstalled(), "jersey2-guice is not installed");

    String url = "http://localhost:" + HttpServerUtils.PORT;

    String[] paths = {
        MyResource.PATH,
        MyAsyncResource.PATH,
        MyFilter.PATH,
        MyHttpServlet.PATH,
        MyQualifierResource.PATH
    };

    String[] responses = {
        MyResource.RESPONSE,
        MyAsyncResource.RESPONSE,
        MyFilter.RESPONSE,
        MyHttpServlet.RESPONSE,
        MyQualifierResource.RESPONSE
    };

    assertEquals(paths.length, responses.length);

    // Client #1: Create a new Client instance for each request
    for (int i = 0; i < paths.length; i++) {
      Client client = ClientBuilder.newClient();
      try {

        WebTarget target = client.target(url).path(paths[i]);

        String value = target.request(MediaType.TEXT_PLAIN).get(String.class);
        assertEquals(value, String.format(responses[i], VALUE));
      } catch (Exception err) {
        fail("Path: " + paths[i], err);
      } finally {
        client.close();
      }
    }

    // Client #2: Re-Use the same Client instance for each request
    Client client = ClientBuilder.newClient();
    try {
      for (int i = 0; i < paths.length; i++) {
        try {
          WebTarget target = client.target(url).path(paths[i]);

          String value = target.request(MediaType.TEXT_PLAIN).get(String.class);
          assertEquals(value, String.format(responses[i], VALUE));
        } catch (Exception err) {
          fail("Path: " + paths[i], err);
        }
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
}
