package noza.core.tls;

import noza.base.exception.TlsException;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.*;

public class TlsContext
{
    private SSLContext sslContext;

    public TlsContext(String keyStore,   String keyStorePassword, String keyStoreKeyPassword,
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

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        }
        catch (Exception e) {
            throw new TlsException(e);
        }
    }
}
