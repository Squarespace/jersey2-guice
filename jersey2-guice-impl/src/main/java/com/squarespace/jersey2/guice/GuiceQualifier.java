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

package com.squarespace.jersey2.guice;

import java.io.Serializable;
import java.lang.annotation.Annotation;

import javax.inject.Qualifier;

import com.google.inject.Guice;
import com.google.inject.Key;

/**
 * A {@link Qualifier} that is backed by a {@link Guice} {@link Key}.
 *
 * NOTE: This class exists just for completeness and debugging.
 *
 * @see Qualifier
 * @see Key
 */
@SuppressWarnings("all")
class GuiceQualifier<T> implements Qualifier, Serializable {

  private static final long serialVersionUID = 0;

  private final Key<T> key;

  public GuiceQualifier(Key<T> key) {
    this.key = key;
  }

  /**
   * Returns the {@link Key}.
   */
  public Key<T> getKey() {
    return key;
  }

  @Override
  public Class<? extends Annotation> annotationType(){
    return key.getAnnotationType();
  }

  @Override
  public int hashCode(){
    return annotationType().hashCode();
  }

  @Override
  public boolean equals(Object o){
    if (o == this) {
      return true;
      
    } else if (annotationType().isInstance(o)) {
      return true;
      
    } else if (!(o instanceof GuiceQualifier<?>)) {
      return false;
    }
    
    GuiceQualifier<?> other = (GuiceQualifier<?>) o;
    return annotationType().equals(other.annotationType());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + key + "]";
  }
}