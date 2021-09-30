package com.example.template.shared.security.truststore;

import com.example.template.shared.security.truststore.TrustManagerFactoryImpl.PkixFactory;
import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@SuppressWarnings("UnsynchronizedOverridesSynchronized")
@EqualsAndHashCode(callSuper = true)
final class TrustManagerProvider extends Provider {

  @Serial private static final long serialVersionUID = 3586598536137795666L;
  private static final String info = "TrustManager provider (SunX509/PKIX trust factories)";
  @Getter private final KeyStore trustStore;
  @Getter private final boolean ignoreDomain;

  TrustManagerProvider(@NonNull KeyStore trustStore, boolean ignoreDomain) {
    super("TrustManager", "1.0", info);

    this.trustStore = trustStore;
    this.ignoreDomain = ignoreDomain;

    ps("SunX509", TrustManagerFactoryImpl.SimpleFactory.class.getName(), null);
    ps("PKIX", PkixFactory.class.getName(), List.of("SunPKIX", "X509", "X.509"));
  }

  private void ps(String algorithm, String className, List<String> aliases) {
    putService(new TrustManagerService(this, algorithm, className, aliases));
  }

  private static class TrustManagerService extends Service {

    TrustManagerService(
        TrustManagerProvider provider, String algorithm, String className, List<String> aliases) {
      super(provider, "TrustManagerFactory", algorithm, className, aliases, null);
    }

    @Override
    public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
      assert constructorParameter == null : "Invalid constructor parameter";

      try {
        var clazz = Class.forName(getClassName());
        var constructor = (Constructor<?>) clazz.getConstructor(TrustManagerProvider.class);

        return constructor.newInstance(getProvider());
      } catch (ClassNotFoundException
          | NoSuchMethodException
          | InstantiationException
          | IllegalAccessException
          | InvocationTargetException ex) {
        throw new NoSuchAlgorithmException(ex);
      }
    }
  }
}
