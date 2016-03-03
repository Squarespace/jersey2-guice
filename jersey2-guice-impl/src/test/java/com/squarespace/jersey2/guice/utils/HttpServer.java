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

package com.squarespace.jersey2.guice.utils;

import java.io.Closeable;
import java.io.IOException;

import org.eclipse.jetty.server.Server;

public class HttpServer implements Closeable {

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
