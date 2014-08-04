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

import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Injector;

// NOTE: This class must be public. Jersey/HK2 will ignore
// it if it's package private or something like that!

@Path(MyResource.PATH)
public class MyResource {

  public static final String PATH = "/rsrc";
  
  public static final String RESPONSE = "MyResource.RESPONSE: %s";
  
  @Inject
  private Injector injector;
  
  @Inject
  @Named(JerseyGuiceTest.NAME)
  private String value;
  
  @Inject
  private Items items;
  
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String sayHello() {
    Items other = injector.getInstance(Items.class);
    
    assertNotSame(other, items);
    assertSame(other.request, items.request);
    
    return String.format(RESPONSE, value);
  }
}