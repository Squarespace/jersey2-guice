package com.squarespace.jersey2.guice;

import javax.inject.Inject;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.glassfish.hk2.api.ServiceLocator;

class Items {

  @Inject
  public ServiceLocator locator;
  
  @Inject
  public Application application;
  
  @Inject
  public Providers providers;
  
  @Inject
  public UriInfo uriInfo;
  
  @Inject
  public HttpHeaders headers;
  
  @Inject
  public SecurityContext securityContext;
  
  @Inject
  public Request request;
}
