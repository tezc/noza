package noza.base.transport.listener;

import noza.base.exception.TlsException;
import noza.base.transport.sock.SSLSock;
import noza.base.transport.sock.Sock;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;

public class TlsListener extends Listener
{
    private static final String PROTOCOL = "tls";

    private final SSLEngine engine;



    public TlsListener(ListenerOwner owner, Selector selector,
                       String hostname,   int port,
                       String keyStore,   String keyStorePassword, String keyStoreKeyPassword,
                       String trustStore, String trustStorePassword)
    {
        super(owner, selector, PROTOCOL, hostname, port);

        try {
            // First initialize the key and trust material
            KeyStore ksKeys = KeyStore.getInstance("JKS");
            ksKeys.load(new FileInputStream(keyStore), keyStorePassword.toCharArray());
            KeyStore ksTrust = KeyStore.getInstance("JKS");
            ksTrust.load(new FileInputStream(trustStore), trustStorePassword.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, keyStoreKeyPassword.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ksTrust);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
        }
        catch (Exception e) {
            throw new TlsException(e);
        }
    }

    @Override
    public Sock accept()
    {
        try {
            SocketChannel incoming = channel.accept();
            incoming.configureBlocking(false);

            return new SSLSock(incoming, engine, false);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String getProtocol()
    {
        return null;
    }
}
