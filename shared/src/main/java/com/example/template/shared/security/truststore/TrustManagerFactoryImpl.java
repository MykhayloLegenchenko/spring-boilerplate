package com.example.template.shared.security.truststore;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.security.cert.CertificateException;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

abstract class TrustManagerFactoryImpl extends TrustManagerFactorySpi {
  private final TrustManagerProvider trustManagerProvider;
  private TrustManagerFactory trustFactory;
  private final TrustManagerFactory platformFormFactory;

  TrustManagerFactoryImpl(TrustManagerProvider provider) throws NoSuchAlgorithmException {
    trustManagerProvider = provider;

    var service = getService();
    if (provider.isIgnoreDomain()) {
      trustFactory = createFactory(service);
    }

    platformFormFactory = createFactory(service);
  }

  @Override
  protected void engineInit(KeyStore keyStore) throws KeyStoreException {
    var trustStore = trustManagerProvider.getTrustStore();
    if (trustFactory != null) {
      trustFactory.init(trustStore);
      platformFormFactory.init(keyStore);
      return;
    }

    var store = KeyStore.getInstance(KeyStore.getDefaultType());
    try {
      store.load(null);
    } catch (IOException | NoSuchAlgorithmException | CertificateException ex) {
      throw new KeyStoreException(ex);
    }

    addStore(store, trustStore);

    if (keyStore == null) {
      addPlatformIssuers(store);
    } else {
      addStore(store, keyStore);
    }

    platformFormFactory.init(store);
  }

  @Override
  protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
      throws InvalidAlgorithmParameterException {
    try {
      if (trustFactory == null) {
        trustFactory = createFactory(getService());
      }

      trustFactory.init(trustManagerProvider.getTrustStore());
    } catch (NoSuchAlgorithmException | KeyStoreException ex) {
      throw new InvalidAlgorithmParameterException(ex);
    }

    platformFormFactory.init(managerFactoryParameters);
  }

  @Override
  protected TrustManager[] engineGetTrustManagers() {
    var platformManagers = platformFormFactory.getTrustManagers();
    if (trustFactory == null) {
      return platformManagers;
    }

    assert platformManagers != null
        && platformManagers.length == 1
        && platformManagers[0] instanceof X509ExtendedTrustManager;

    var trustManagers = trustFactory.getTrustManagers();
    assert trustManagers != null
        && trustManagers.length == 1
        && trustManagers[0] instanceof X509ExtendedTrustManager;

    var trustManager = (X509ExtendedTrustManager) trustManagers[0];
    if (trustManagerProvider.isIgnoreDomain()) {
      trustManager = new X509ExtendedTrustManagerAdapter(trustManager);
    }

    return new TrustManager[] {
      new ProxyTrustManagerImpl(trustManager, (X509ExtendedTrustManager) platformManagers[0])
    };
  }

  abstract String getAlgorithm();

  private Service getService() {
    var provider = Security.getProvider("SunJSSE");
    assert provider != null : "Cannot get SunJSSE security provider";

    var alg = getAlgorithm();
    var service = provider.getService("TrustManagerFactory", alg);
    assert service != null : "Cannot find service TrustManagerFactory(" + alg + ")";

    return service;
  }

  private TrustManagerFactory createFactory(Service service) throws NoSuchAlgorithmException {
    var instance = service.newInstance(null);
    if (!(instance instanceof TrustManagerFactorySpi spi)) {
      throw new IllegalStateException(
          "Instance class not does not extend TrustManagerFactorySpi: " + instance.getClass());
    }

    return new TrustManagerFactoryProxy(spi, service.getProvider(), getAlgorithm());
  }

  private void addPlatformIssuers(KeyStore dest) throws KeyStoreException {
    TrustManagerFactory tmf;
    try {
      tmf = createFactory(getService());
    } catch (NoSuchAlgorithmException ex) {
      throw new KeyStoreException(ex);
    }

    tmf.init((KeyStore) null);

    var managers = tmf.getTrustManagers();
    if (managers == null || managers.length != 1 || !(managers[0] instanceof X509TrustManager tm)) {
      throw new IllegalStateException("Cannot find X509TrustManager");
    }

    var i = 0;
    for (var cert : tm.getAcceptedIssuers()) {
      dest.setCertificateEntry("platform_" + i, cert);
      i++;
    }
  }

  private static void addStore(KeyStore dest, KeyStore src) throws KeyStoreException {
    var aliases = src.aliases();
    while (aliases.hasMoreElements()) {
      var alias = aliases.nextElement();
      if (src.isCertificateEntry(alias)) {
        dest.setCertificateEntry(alias, src.getCertificate(alias));
      } else if (src.isKeyEntry(alias)) {
        var certs = src.getCertificateChain(alias);
        if (certs != null && certs.length > 0) {
          dest.setCertificateEntry(alias, certs[0]);
        }
      }
    }
  }

  public static final class PkixFactory extends TrustManagerFactoryImpl {

    public PkixFactory(TrustManagerProvider provider) throws NoSuchAlgorithmException {
      super(provider);
    }

    @Override
    protected String getAlgorithm() {
      return "SunX509";
    }
  }

  public static final class SimpleFactory extends TrustManagerFactoryImpl {

    public SimpleFactory(TrustManagerProvider provider) throws NoSuchAlgorithmException {
      super(provider);
    }

    @Override
    protected String getAlgorithm() {
      return "PKIX";
    }
  }

  private static class TrustManagerFactoryProxy extends TrustManagerFactory {

    TrustManagerFactoryProxy(
        TrustManagerFactorySpi factorySpi, Provider provider, String algorithm) {
      super(factorySpi, provider, algorithm);
    }
  }
}
