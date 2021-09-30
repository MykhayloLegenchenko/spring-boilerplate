package com.example.template.account.microservice.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionData implements Serializable {
  @Serial private static final long serialVersionUID = -4886715277671806804L;

  private String refreshToken;

  @Serial
  private void readObject(ObjectInputStream in) throws IOException {
    in.readByte(); // skip version byte
    refreshToken = readString(in);
  }

  @Serial
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.writeByte(1);
    writeString(out, refreshToken);
  }

  private static String readString(ObjectInputStream in) throws IOException {
    short len = in.readShort();
    byte[] bytes = in.readNBytes(len);

    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static void writeString(ObjectOutputStream out, @NonNull String value)
      throws IOException {

    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > Short.MAX_VALUE) {
      throw new IOException("String is too long for serialization");
    }

    out.writeShort(bytes.length);
    out.write(bytes);
  }
}
