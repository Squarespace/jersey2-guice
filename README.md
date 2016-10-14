# Jersey 2.0 w/ Guice

[![Continuous Integration](https://travis-ci.org/Squarespace/jersey2-guice.svg?branch=master)](https://travis-ci.org/Squarespace/jersey2-guice)

## Introduction

This library provides support for Jersey 2.0 w/ Guice similar to the way it used to work in Jersey 1.x with the [jersey-guice](https://jersey.java.net/nonav/apidocs/1.8/contribs/jersey-guice/com/sun/jersey/guice/spi/container/servlet/package-summary.html) library. It uses Guice's own [GuiceFilter](https://google-guice.googlecode.com/git/javadoc/com/google/inject/servlet/GuiceFilter.html) (like jersey-guice) and it's somewhat different from the [Guice/HK2 Bridge](https://hk2.java.net/guice-bridge).

## Installation

Go to [search.maven.org](http://search.maven.org) and search for `g:"com.squarespace.jersey2-guice" AND a:"jersey2-guice-impl"` to find the current release version.

### Gradle

```goovy
compile "com.squarespace.jersey2-guice:jersey2-guice-impl:${current.version}"
```

### Maven

```xml
<dependency>
  <groupId>com.squarespace.jersey2-guice</groupId>
  <artifactId>jersey2-guice-impl</artifactId>
  <version>${current.version}</version>
</dependency>
```

## Usage

### Getting Started

Jersey/HK2 uses unfortunately SPIs and the Singleton pattern internally. Your code is effectively racing against the Servlet container's code and the first one to initialize HK2's `ServiceLocatorGenerator` inside its `ServiceLocatorFactory` wins.

This library uses two approaches to override HK2's own `ServiceLocatorGenerator`. It first tries to use a SPI and if it can't it'll fall back to reflection to replace a `private static` field. Regardless of the approach it's still a race against the Servlet container.


```java
public static void main(String[] args) {

  List<Module> modules = new ArrayList<>();
  
  modules.add(new JerseyGuiceModule("__HK2_Generated_0"));
  modules.add(new ServletModule());
  modules.add(new AbstractModule() {
    @Override
    protected void configure() {
      // ...
    }
  });
  
  Injector injector = Guice.createInjector(modules);
  JerseyGuiceUtils.install(injector);
  
  // ... continue ...
}

```

#### META-INF

To ensure Jersey/HK2 SPIs will use the proper ServiceLocationGenerator, simply copy the META-INF folder into your root project's resources folder.

### Documentation

The [User's Guide](https://github.com/Squarespace/jersey2-guice/wiki) can be found in the Wiki.

## Apache 2.0 License

    Copyright 2014-2016 Squarespace, Inc.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
