package noza.base.transport.listener;


import noza.base.poller.Fd;
import noza.base.transport.sock.TcpSock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.*;

public class TcpListener extends Listener implements Fd
{
    private static final String PROTOCOL = "tcp";


    public TcpListener(ListenerOwner owner,
                       Selector selector, String hostname, int port)
    {
        super(owner, selector, PROTOCOL, hostname, port);
    }

    public void close()
    {
        try {
            channel.close();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public TcpSock accept()
    {
        try {
            SocketChannel incoming = channel.accept();
            incoming.configureBlocking(false);
            incoming.socket().setTcpNoDelay(true);

            return new TcpSock(null, incoming);
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

    @Override
    public String getProtocol()
    {
        return PROTOCOL;
    }
}
