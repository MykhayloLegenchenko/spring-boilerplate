package com.example.template.shared.security.truststore;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import lombok.NonNull;

@SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
class ProxyTrustManagerImpl extends X509ExtendedTrustManager {
  private final X509ExtendedTrustManager manager;
  private final X509ExtendedTrustManager platformManager;

  ProxyTrustManagerImpl(
      @NonNull X509ExtendedTrustManager manager,
      @NonNull X509ExtendedTrustManager platformManager) {
    this.manager = manager;
    this.platformManager = platformManager;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    try {
      manager.checkClientTrusted(chain, authType);
      return;
    } catch (CertificateException ex) {
      // empty
    }

    platformManager.checkClientTrusted(chain, authType);
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
      throws CertificateException {
    try {
      manager.checkClientTrusted(chain, authType, socket);
      return;
    } catch (CertificateException ex) {
      // empty
    }

    platformManager.checkClientTrusted(chain, authType, socket);
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine sslEngine)
      throws CertificateException {
    try {
      manager.checkClientTrusted(chain, authType, sslEngine);
      return;
    } catch (CertificateException ex) {
      // empty
    }

    platformManager.checkClientTrusted(chain, authType, sslEngine);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    try {
      manager.checkServerTrusted(chain, authType);
      return;
    } catch (CertificateException ex) {
      // empty
    }

    platformManager.checkServerTrusted(chain, authType);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
      throws CertificateException {
    try {
      manager.checkServerTrusted(chain, authType, socket);
      return;
    } catch (CertificateException ex) {
      // empty
    }

    platformManager.checkServerTrusted(chain, authType, socket);
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine sslEngine)
      throws CertificateException {
    try {
      manager.checkServerTrusted(chain, authType, sslEngine);
      return;
    } catch (CertificateException ex) {
      // empty
    }

    platformManager.checkServerTrusted(chain, authType, sslEngine);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    var trustCerts = manager.getAcceptedIssuers();
    var platformCerts = platformManager.getAcceptedIssuers();
    var certs = new X509Certificate[trustCerts.length + platformCerts.length];
    System.arraycopy(trustCerts, 0, certs, 0, trustCerts.length);
    System.arraycopy(platformCerts, 0, certs, trustCerts.length, platformCerts.length);

    return certs;
  }
}
