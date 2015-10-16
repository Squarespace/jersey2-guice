package com.squarespace.jersey2.guice.resource;

public class HelloServiceImpl implements HelloService {

  public static final String DEFAULT_HELLO = "hello";

  public static final String ANNOTATED_HELLO = "hello (annotaded)";
  
  public static final String NAMED_HELLO = "hello (named)";
  
  private final String value;
  
  public HelloServiceImpl(String value) {
    this.value = value;
  }
  
  @Override
  public String hello() {
    return value;
  }
}
