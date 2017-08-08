package noza.base.transport.listener;

import noza.base.config.Config;
import noza.base.poller.Fd;
import noza.base.transport.sock.Sock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;

public abstract class Listener implements Fd
{
    protected final String uri;
    protected final ServerSocketChannel channel;
    protected final SelectionKey key;
    protected final Selector selector;
    protected final ListenerOwner owner;

    protected Listener(ListenerOwner owner,
                       Selector selector, String protocol, String hostname, int port)
    {
        this.owner    = owner;
        this.selector = selector;
        this.uri      = protocol + "://" + hostname + ":" + port;

        try {
            channel = ServerSocketChannel.open();
            channel.bind(new InetSocketAddress(hostname, port));
            channel.socket().setReuseAddress(true);
            channel.configureBlocking(false);

            key = channel.register(selector, SelectionKey.OP_ACCEPT, this);

        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString()
    {
        return uri;
    }

    @Override
    public void onAccept()
    {
        owner.handleAcceptEvent(this);
    }


    public abstract Sock accept();
    public abstract String getProtocol();

    public static Listener create(ListenerOwner owner,
                                  Selector selector, Map<Config, Object> configs)
    {
        String protocol = (String) configs.get(Config.TRANSPORT_PROTOCOL);
        switch (protocol) {
            case "tcp":
                return new TcpListener(owner, selector,
                                       (String) configs.get(Config.TRANSPORT_HOSTNAME),
                                       (int)    configs.get(Config.TRANSPORT_PORT));
            case "tls":
                return new TlsListener(owner, selector,
                                       (String) configs.get(Config.TRANSPORT_HOSTNAME),
                                       (int)    configs.get(Config.TRANSPORT_PORT),
                                       (String) configs.get(Config.TRANSPORT_KEYSTORE),
                                       (String) configs.get(Config.TRANSPORT_KEYSTORE_PASSWORD),
                                       (String) configs.get(Config.TRANSPORT_KEYSTORE_KEY_PASSWORD),
                                       (String) configs.get(Config.TRANSPORT_TRUSTSTORE),
                                       (String) configs.get(Config.TRANSPORT_TRUSTSTORE_PASSWORD)
                                       );
            case "ws":
                return new WsListener(owner, selector,
                                      (String) configs.get(Config.TRANSPORT_HOSTNAME),
                                      (int) configs.get(Config.TRANSPORT_PORT),
                                      (String) configs.get(Config.TRANSPORT_URI)
                );


        }

        return null;

    }
}
