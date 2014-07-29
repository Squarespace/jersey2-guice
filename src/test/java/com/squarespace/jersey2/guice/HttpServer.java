package com.squarespace.jersey2.guice;

import java.io.Closeable;
import java.io.IOException;

import org.eclipse.jetty.server.Server;

class HttpServer implements Closeable {

  private final Server server;

  public HttpServer(Server server) {
    this.server = server;
  }
  
  @Override
  public void close() throws IOException {
    try {
      server.stop();
    } catch (Exception err) {
      throw new IOException(err);
    }
  }
}
