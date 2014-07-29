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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.internal.ServiceLocatorFactoryImpl;
import org.glassfish.jersey.internal.inject.Injections;
import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.squarespace.jersey2.guice.spi.GuiceServiceLocatorGeneratorSPI;

/**
 * This utility class provides some brute-force setter methods to make HK2 work 
 * with {@link Guice}. The underlying problem is that Jersey is written with 
 * extensibility in mind but totally fails to follow though with it. Some of the 
 * key classes are hard coded as {@code private static final} fields. There is 
 * an open JIRA for it.
 * 
 * @see https://java.net/jira/browse/JERSEY-2551
 */
class InjectionsUtils {

  private static final Logger LOG = LoggerFactory.getLogger(InjectionsUtils.class);
  
  private static final String GENERATOR_FIELD = "generator";
  
  private static final String FACTORY_FIELD = "factory";
  
  private static final String INSTANCE_FIELD = "INSTANCE";
  
  private static final String MODIFIERS_FIELD = "modifiers";
  
  /**
   * Returns {@code true} if JERSEY-2551 is fixed.
   */
  public static boolean hasFix() {
    try {
      Injections.class.getDeclaredField(GENERATOR_FIELD);
      return false;
    } catch (NoSuchFieldException err) {
      return true;
    }
  }
  
  /**
   * Installs the given {@link ServiceLocatorGenerator}.
   */
  public static void setServiceLocatorGenerator(ServiceLocatorGenerator generator) {
    try {
      install(Injections.class, GENERATOR_FIELD, generator);
    } catch (NoSuchFieldException err) {
      LOG.trace("NoSuchFieldException: JERSEY-2551. This OK if you're using Jersey 2.11+", err);
    
      installGeneratorSPI(generator);
    }
  }
  
  /**
   * Installs the given {@link ServiceLocatorGenerator}.
   * 
   * @see GuiceServiceLocatorGeneratorSPI
   */
  public static void installGeneratorSPI(ServiceLocatorGenerator generator) {
    ServiceLocatorGenerator previous 
      = GuiceServiceLocatorGeneratorSPI.install(generator);
  
    if (previous != null && LOG.isWarnEnabled()) {
      LOG.warn("Replaced ServiceLocatorGenerator (OK if testing): previous={}, generator={}", previous, generator);
    }
  }
  
  /**
   * Installs the given {@link ServiceLocatorFactory}.
   */
  public static void setServiceLocatorFactory(ServiceLocatorFactory factory) {
    try {
      install(ServiceLocatorFactory.class, INSTANCE_FIELD, factory);
      install(Injections.class, FACTORY_FIELD, factory);
    } catch (NoSuchFieldException err) {
      throw new IllegalStateException(err);
    }
  }
  
  /**
   * Returns {@code true} if there is a non {@link ServiceLocatorGeneratorImpl} installed.
   */
  public static boolean isGeneratorInstalled() {
    return !equals(Injections.class, GENERATOR_FIELD, ServiceLocatorGeneratorImpl.class);
  }
  
  /**
   * Returns {@code true} if there is a non {@link ServiceLocatorFactoryImpl} installed.
   */
  public static boolean isFactoryInstalled() {
    return !equals(ServiceLocatorFactory.class, INSTANCE_FIELD, ServiceLocatorFactoryImpl.class)
        && !equals(Injections.class, FACTORY_FIELD, ServiceLocatorFactoryImpl.class);
  }
  
  private static void install(Class<?> clazz, String name, Object value) throws NoSuchFieldException {
    try {
      Field field = clazz.getDeclaredField(name);
      field.setAccessible(true);
      
      // Turn off the 'final' if necessary!
      int modifiers = field.getModifiers();
      if (Modifier.isFinal(modifiers)) {
        setModifiers(field, modifiers & ~Modifier.FINAL);
        try {
          field.set(null, value);
        } finally {
          setModifiers(field, modifiers | Modifier.FINAL);
        }
      } else {
        field.set(null, value);
      }
      
    } catch (IllegalAccessException err) {
      throw new IllegalStateException(err);
    }
  }
  
  private static void setModifiers(Field dst, int modifiers) throws IllegalAccessException, NoSuchFieldException, SecurityException {
    Field field = Field.class.getDeclaredField(MODIFIERS_FIELD);
    field.setAccessible(true);
    field.setInt(dst, modifiers);
  }
  
  private static boolean equals(Class<?> clazz, String name, Class<?> type) {
    Object value = getValue(clazz, name);
    if (value != null && value.getClass().equals(type)) {
      return true;
    }
    
    return false;
  }
  
  private static Object getValue(Class<?> clazz, String name) {
    try {
      Field field = clazz.getDeclaredField(name);
      field.setAccessible(true);
      return field.get(null);
    } catch (NoSuchFieldException | IllegalAccessException err) {
      throw new IllegalStateException(err);
    }
  }
  
  private InjectionsUtils() {}
}
