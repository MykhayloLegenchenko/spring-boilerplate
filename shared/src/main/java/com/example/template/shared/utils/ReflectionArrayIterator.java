package com.example.template.shared.utils;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;
import lombok.NonNull;

public class ReflectionArrayIterator implements Iterator<Object> {
  private final Object array;
  private int index;
  private final int length;

  public ReflectionArrayIterator(@NonNull Object array) {
    var arrayType = array.getClass();
    if (!arrayType.isArray()) {
      throw new IllegalArgumentException("Incorrect array type: " + arrayType);
    }

    this.array = array;
    length = Array.getLength(array);
  }

  @Override
  public boolean hasNext() {
    return index < length;
  }

  @Override
  public Object next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more elements");
    }

    return Array.get(array, index++);
  }
}
