package com.squarespace.jersey2.guice;

import java.io.IOException;
import java.util.EnumSet;
import java.util.EventListener;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.google.inject.servlet.GuiceFilter;

class HttpServerUtils {

  public static final int PORT = 8081;
  
  private HttpServerUtils() {}
  
  public static HttpServer newHttpServer(Class<?> rsrc) throws IOException {
    return newHttpServer(rsrc, null);
  }
  
  public static HttpServer newHttpServer(Class<?> rsrc, EventListener listener) throws IOException {
    ResourceConfig config = new ResourceConfig();
    
    config.register(rsrc);
    
    ServletContainer servletContainer = new ServletContainer(config);
    
    ServletHolder sh = new ServletHolder(servletContainer);
    Server server = new Server(PORT);
    ServletContextHandler context = new ServletContextHandler(
        ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    
    FilterHolder filterHolder = new FilterHolder(GuiceFilter.class);
    context.addFilter(filterHolder, "/*", 
        EnumSet.allOf(DispatcherType.class));
    
    context.addServlet(sh, "/*");
    
    if (listener != null) {
      context.addEventListener(listener);
    }
    
    server.setHandler(context);
    
    try {
      server.start();
    } catch (Exception err) {
      throw new IOException(err);
    }
    
    return new HttpServer(server);
  }
}
