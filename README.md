# Jersey 2.0 w/ Guice

This library provides support for Jersey 2.0 w/ Guice similar to the way it used to work in Jersey 1.x with the [jersey-guice](https://jersey.java.net/nonav/apidocs/1.8/contribs/jersey-guice/com/sun/jersey/guice/spi/container/servlet/package-summary.html) library. It uses Guice's own [GuiceFilter](https://google-guice.googlecode.com/git/javadoc/com/google/inject/servlet/GuiceFilter.html) (like jersey-guice) and it's somewhat different from the [Guice/HK2 Bridge](https://hk2.java.net/guice-bridge).

[![Continuous Integration](https://travis-ci.org/Squarespace/jersey2-guice.svg?branch=master)](https://travis-ci.org/Squarespace/jersey2-guice)

## Differences

### Differences to Jersey 1.x w/ Guice

This library does **NOT** use Guice's [GuiceServletContextListener](https://google-guice.googlecode.com/git/javadoc/com/google/inject/servlet/GuiceServletContextListener.html) to get things started. We use a slightly different [ServletContextListener](https://github.com/Squarespace/jersey2-guice/blob/master/src/main/java/com/squarespace/jersey2/guice/JerseyGuiceServletContextListener.java) implementation to bootstrap Guice and there are also two alternative approaches available (see [below](https://github.com/Squarespace/jersey2-guice#example)).

### Differences to Guice/HK2 Bridge

The Guice/HK2 Bridge has incomplete support for Guice (from a Guice user's perspective). It's OK for injecting Guice services into [HK2](https://hk2.java.net) but you'll probably not be very happy with the other way around. 

#### Injecting Guice services into HK2

It works on the HK2 side as long as you don't use any of the following:

1. `javax.annotation.Nullable` ([JSR-305](https://jcp.org/en/jsr/detail?id=305))
2. [`com.google.inject.name.Named`](https://google-guice.googlecode.com/git/javadoc/com/google/inject/name/Named.html)
3. [`com.google.inject.Inject`](https://google-guice.googlecode.com/git/javadoc/com/google/inject/Inject.html)
4. Any custom [`BindingAnnotation`](https://code.google.com/p/google-guice/wiki/BindingAnnotations)

HK2 doesn't know anything about these annotations (they're not [Qualifiers](http://docs.oracle.com/javaee/6/api/javax/inject/Qualifier.html)) and ignores them as it's constructing the [Injectee](https://hk2.java.net/apidocs/org/glassfish/hk2/api/Injectee.html) (the equivalent of a [Key](https://google-guice.googlecode.com/git/javadoc/com/google/inject/Key.html) in Guice). If you're a Guice user you probably want to use all of them in your Jersey endpoints.

#### Injecting HK2 services into Guice

Injecting [UriInfo](https://jsr311.java.net/nonav/javadoc/javax/ws/rs/core/UriInfo.html) & Co. with `@HK2Inject` is not fun.

## How to use it

### Gradle

```
compile "com.squarespace.jersey2-guice:jersey2-guice:${current.version}"
```

### Maven

```
<dependency>
  <groupId>com.squarespace.jersey2-guice</groupId>
  <artifactId>jersey2-guice</artifactId>
  <version>${current.version}</version>
</dependency>
```

### Available Injections

The following items are available for injection (on top of [ServletModule's](https://github.com/google/guice/wiki/ServletModule) bindings). You can customize the list by extracting more things from the [ServiceLocator](https://hk2.java.net/nonav/hk2-api/apidocs/org/glassfish/hk2/api/ServiceLocator.html).

1. org.glassfish.hk2.api.ServiceLocator
2. javax.ws.rs.core.Application
3. javax.ws.rs.ext.Providers
4. javax.ws.rs.core.UriInfo
5. javax.ws.rs.core.HttpHeaders
6. javax.ws.rs.core.SecurityContext
7. javax.ws.rs.core.Request

### Example

```
// Guice Module
AbstractModule module = new AbstractModule() {
  @Override
  protected void configure() {
    bind(String.class)
      .annotatedWith(Names.named("name"))
      .toInstance("Hello, World!");
  }
};
```

```
// Option #1 (Classic) w/ ServletContextListener
public class MyApplication extends JerseyGuiceServletContextListener {

  @Override
  protected List<? extends Module> modules() {
    return Arrays.asList(module);
  }
}
```

```
META-INF/services/org.glassfish.hk2.extension.ServiceLocatorGenerator
  -> my.package.MyApplication

// Option #2 w/ ServiceLocatorGenerator (SPI)
public class MyApplication extends JerseyServiceLocatorGeneratorSPI {

  @Override
  protected List<? extends Module> modules() {
    return Arrays.asList(module);
  }
}
```

```
// Option #3 w/ Reflection
public static void main(String[] args) {
  ServiceLocator locator = BootstrapUtils.newServiceLocator();
  Injector injector = BootstrapUtils.newInjector(locator, Arrays.asList(module));
  
  BootstrapUtils.install(locator);

  // Start Jetty!
}
```

```
@Path("/hello")
public class MyResource {

  @Inject
  @Named("name")
  private String value;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String sayHello() {
    return value;
  }
}

```

### AOP

```
@Path("/my-resource")
public class MyResource {
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @MyAnnotation
  public String sayHello() {
    return "Hello, World!";
  }
}

AbstractModule module = new AbstractModule() {
  @Override
  protected void configure() {
    GuiceBinding.bind(binder(), MyResource.class);

    bindInterceptor(Matchers.any(), 
        Matchers.annotatedWith(MyAnnotation.class), 
        <<MethodInterceptor>>);
  }
};

```

## How it works

If you're a longtime Guice & Jersey user then you've probably come across the issue with Jersey 2.0.

The root of all the trouble is [JERSEY-2551](https://java.net/jira/browse/JERSEY-2551). A lot of HK2 is written with [SPIs](http://en.wikipedia.org/wiki/Service_provider_interface) and extensibility in mind but deep down it's hard-coded to some `private static final` factories that cannot be changed with no other means than brute-force reflection. Issue JERSEY-2551 has been fixed in Jersey 2.11+ and this library supports older versions (e.g. 2.9.x) and 2.11+.


# Apache 2.0 License

    Copyright 2014 Squarespace, Inc.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
