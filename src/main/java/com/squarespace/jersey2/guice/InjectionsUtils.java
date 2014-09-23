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
import java.util.ArrayList;
import java.util.List;

import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.jersey.internal.inject.Injections;

import com.google.inject.Guice;

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
  
  private static final String GENERATOR_FIELD = "generator";
  
  private static final String MODIFIERS_FIELD = "modifiers";
  
  private static final String DEFAULT_GENERATOR_FIELD = "defaultGenerator";
  
  /**
   * Returns {@code true} if JERSEY-2551 is fixed.
   * 
   * Should be {@code true} for Jersey 2.11+
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
   * Installs the given {@link ServiceLocatorGenerator} using reflection.
   */
  public static void install(ServiceLocatorGenerator generator) {
    try {
      if (!hasFix()) {
        Field field = Injections.class.getDeclaredField(GENERATOR_FIELD);
        set(field, null, generator);
      }
      
      ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
      Class<?> clazz = factory.getClass();
      Field field = clazz.getDeclaredField(DEFAULT_GENERATOR_FIELD);
      set(field, factory, generator);
      
    } catch (NoSuchFieldException | IllegalAccessException | SecurityException err) {
      throw new IllegalStateException(err);
    }
  }
  
  /**
   * Returns the currently installed {@link ServiceLocatorGenerator}s.
   */
  public static List<ServiceLocatorGenerator> getServiceLocatorGenerators() {
    List<ServiceLocatorGenerator> dst = new ArrayList<>();
    
    try {
      
      if (!hasFix()) {
        Field field = Injections.class.getDeclaredField(GENERATOR_FIELD);
        ServiceLocatorGenerator generator = (ServiceLocatorGenerator)get(field, null);
        
        if (generator != null) {
          dst.add(generator);
        }
      }
      
      ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
      Class<?> clazz = factory.getClass();
      
      Field field = clazz.getDeclaredField(DEFAULT_GENERATOR_FIELD);
      ServiceLocatorGenerator generator = (ServiceLocatorGenerator)get(field, factory);
    
      if (generator != null) {
        dst.add(generator);
      }
      
    } catch (NoSuchFieldException | IllegalAccessException | SecurityException err) {
      throw new IllegalStateException(err);
    }
    
    return dst;
  }
  
  private static void set(Field field, Object instance, Object value) throws  IllegalAccessException, NoSuchFieldException, SecurityException {
    field.setAccessible(true);
    
    int modifiers = field.getModifiers();
    if (Modifier.isFinal(modifiers)) {
      setModifiers(field, modifiers & ~Modifier.FINAL);
      try {
        field.set(instance, value);
      } finally {
        setModifiers(field, modifiers | Modifier.FINAL);
      }
    } else {
      field.set(instance, value);
    }
  }
  
  private static Object get(Field field, Object instance) throws  IllegalAccessException, NoSuchFieldException, SecurityException {
    field.setAccessible(true);
    return field.get(instance);
  }
  
  private static void setModifiers(Field dst, int modifiers) throws IllegalAccessException, NoSuchFieldException, SecurityException {
    Field field = Field.class.getDeclaredField(MODIFIERS_FIELD);
    field.setAccessible(true);
    field.setInt(dst, modifiers);
  }
  
  private InjectionsUtils() {}
}
