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
  
  public static HttpServer newHttpServer(Class<?>... rsrc) throws IOException {
    return newHttpServer((EventListener)null, rsrc);
  }
  
  public static HttpServer newHttpServer(EventListener listener, Class<?>... rsrc) throws IOException {
    ResourceConfig config = new ResourceConfig();
    
    for (Class<?> clazz : rsrc) {
      config.register(clazz);
    }
    
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
