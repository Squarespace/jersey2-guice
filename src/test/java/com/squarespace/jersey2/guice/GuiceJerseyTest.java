package com.squarespace.jersey2.guice;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
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

import org.glassfish.hk2.api.ServiceLocator;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;
import com.squarespace.jersey2.guice.BootstrapUtils;
import com.squarespace.jersey2.guice.GuiceServletContextListener;

public class GuiceJerseyTest {

  public static final String NAME = "JerseyGuiceTest.NAME";
  
  private static final String VALUE = "Hello, World!";
  
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
  
  @Test
  public void useSPI() throws IOException {
    embedded(false);
    classic(false);
  }
  
  @Test
  public void useReflection() throws IOException {
    embedded(true);
    classic(true);
  }
  
  private void embedded(boolean useReflection) throws IOException {
    
    ServiceLocator locator = BootstrapUtils.newServiceLocator();
    
    @SuppressWarnings("unused")
    Injector injector = BootstrapUtils.newInjector(locator, Arrays.asList(jerseyModule, customModule));
    
    BootstrapUtils.install(locator, useReflection);
    
    try (HttpServer server = HttpServerUtils.newHttpServer(MyResource.class)) {
      check();
    }
  }
  
  private void classic(boolean useReflection) throws IOException {
    
    System.setProperty(GuiceServletContextListener.USE_REFLECTION, Boolean.toString(useReflection));
    
    final AtomicInteger counter = new AtomicInteger();
    
    ServletContextListener listener = new ServletContextListener() {
      @Override
      public void contextInitialized(ServletContextEvent sce) {
        (new GuiceServletContextListener() {
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
  
  private void check() throws IOException {
    
    String url = "http://localhost:" + HttpServerUtils.PORT;
    
    String[] paths = { MyResource.PATH, MyFilter.PATH, MyHttpServlet.PATH };
    String[] responses = { MyResource.RESPONSE, MyFilter.RESPONSE, MyHttpServlet.RESPONSE };
    
    assertEquals(paths.length, responses.length);
    
    // Create a new Client instance for each request
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
    
    // Re-Use the same Client instance for each request
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
}
