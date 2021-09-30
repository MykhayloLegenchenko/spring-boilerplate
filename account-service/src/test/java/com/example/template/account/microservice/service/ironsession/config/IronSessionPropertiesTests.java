package com.example.template.account.microservice.service.ironsession.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.template.shared.model.LinkedStringMap;
import org.junit.jupiter.api.Test;

class IronSessionPropertiesTests {
  @Test
  void testSetPasswords() throws Exception {
    var properties = new IronSessionProperties();
    assertTrue(properties.getCurrentPassword().isEmpty());

    var password = "3GDp4QSrAPPzdPKbEhLZrgXhKtS-0WjF";
    properties.setPasswords(password);

    var current = properties.getCurrentPassword();
    assertTrue(current.isPresent());
    assertEquals("1", current.get().getKey());

    String[] passwords = {"DAcPzOlZyd3SkQs01jptRJ1lBafUvPHB", "DAcPzOlZyd3SkQs01jptRJ1lBafUvPHB"};
    properties.setPasswords("{\"k2\":\"" + passwords[1] + "\", \"k1\":\"" + passwords[0] + "\"}");

    var passwordsMap = new LinkedStringMap();
    passwordsMap.put("k2", passwords[1]);
    passwordsMap.put("k1", passwords[0]);

    assertArrayEquals(
        passwordsMap.entrySet().toArray(), properties.getPasswordsMap().entrySet().toArray());
  }
}
