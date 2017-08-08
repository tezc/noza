package noza.base.transport.listener;

import noza.base.poller.Fd;
import noza.base.transport.sock.WsSock;
import noza.base.transport.sock.Sock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class WsListener extends Listener implements Fd
{
    private static final String PROTOCOL = "ws";
    private String requestUri;

    public WsListener(ListenerOwner owner, Selector selector,
                      String hostname, int port, String requestUri)
    {
        super(owner, selector, PROTOCOL, hostname, port);

        this.requestUri = requestUri;
    }

    public void setRequestUri(String requestUri)
    {
        this.requestUri = requestUri;
    }

    public String getRequestUri()
    {
        return requestUri;
    }

    @Override
    public String getProtocol()
    {
        return PROTOCOL;
    }

    public Sock accept()
    {
        try {
            SocketChannel incoming = channel.accept();
            incoming.configureBlocking(false);

            return new WsSock(null, incoming, this);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void onAccept()
    {
        owner.handleAcceptEvent(this);
    }
}
