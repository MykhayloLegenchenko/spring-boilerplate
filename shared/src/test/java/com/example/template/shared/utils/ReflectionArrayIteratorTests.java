package com.example.template.shared.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class ReflectionArrayIteratorTests {
  @Test
  void intArrayIteration() {
    int[] array = {3, 1, 2};
    var it = new ReflectionArrayIterator(array);

    assertIteratorValues(array, it);
  }

  @Test
  void stringArrayIteration() {
    String[] array = {"3", "1", null};
    var it = new ReflectionArrayIterator(array);

    assertIteratorValues(array, it);
  }

  private static void assertIteratorValues(@NonNull Object array, Iterator<?> it) {
    var length = Array.getLength(array);
    if (length != 0) {
      for (var i = 0; i < length; i++) {
        assertTrue(it.hasNext());

        var value = Array.get(array, i);
        assertEquals(value, it.next());
      }
    }

    assertFalse(it.hasNext());
    assertThrows(NoSuchElementException.class, it::next);
  }
}
