package noza.core.tls;


import noza.base.exception.TlsException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.*;

public class TlsEngine
{
    private SSLEngine engine;

    public TlsEngine(String hostname,   int port,
                     String keyStore,   String keyStorePassword, String keyStoreKeyPassword,
                     String trustStore, String trustStorePassword)
    {

        try {
            // First initialize the key and trust material
            KeyStore ksKeys = KeyStore.getInstance("JKS");
            ksKeys.load(new FileInputStream(keyStore), keyStorePassword.toCharArray());
            KeyStore ksTrust = KeyStore.getInstance("JKS");
            ksTrust.load(new FileInputStream(trustStore), trustStorePassword.toCharArray());

            // KeyManagers decide which key material to use
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, keyStoreKeyPassword.toCharArray());

            // TrustManagers decide whether to allow connections
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ksTrust);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // Create the engine
            engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
        }
        catch (Exception e) {
            throw new TlsException(e);
        }
    }
}
