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

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ManagedAsync;

@Path(MyAsyncResource.PATH)
public class MyAsyncResource {

  public static final String PATH = "/async-rsrc";
  
  public static final String RESPONSE = "MyAsyncResource.RESPONSE: %s";
  
  @Inject
  @Named(JerseyGuiceTest.NAME)
  private String value;
  
  @GET
  @ManagedAsync
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.TEXT_PLAIN)
  public void sayAsyncHello(@Suspended final AsyncResponse response) {
    (new Thread() {
      @Override
      public void run() {
        response.resume(String.format(RESPONSE, value));
      }
    }).start();
  }
}