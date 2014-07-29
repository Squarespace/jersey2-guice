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
  @Named(GuiceJerseyTest.NAME)
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