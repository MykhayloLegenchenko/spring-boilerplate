package com.example.template.shared.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import lombok.NonNull;

public final class ObjectUtilities {
  private ObjectUtilities() {}

  public static byte[] toBytes(@NonNull Serializable value) throws IOException {
    try (var bos = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(bos)) {
      oos.writeObject(value);
      oos.flush();
      return bos.toByteArray();
    }
  }

  public static Object fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
    try (var bis = new ByteArrayInputStream(bytes);
        var ois = new ObjectInputStream(bis)) {
      return ois.readObject();
    }
  }
}
