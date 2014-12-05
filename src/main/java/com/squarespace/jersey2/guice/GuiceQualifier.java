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
  public Class<? extends Annotation> annotationType() {
    return Qualifier.class;
  }
  
  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof GuiceQualifier<?>)) {
      return false;
    }
    
    GuiceQualifier<?> other = (GuiceQualifier<?>)o;
    return key.equals(other.key);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + key + "]";
  }
}