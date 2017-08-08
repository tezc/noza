package noza.base.transport.sock;

import noza.base.poller.Fd;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;


public abstract class Sock implements Fd
{
    protected String localAddress;
    protected String remoteAddress;
    protected String str;
    protected final SocketChannel channel;
    protected SelectionKey key;
    protected int ops;
    protected SockOwner owner;
    protected ByteBuffer recvBuf;
    protected ByteBuffer sendBuf;
    boolean connected;


    protected Sock(SockOwner owner, SocketChannel channel, String protocol)
    {
        this.owner       = owner;
        this.channel     = channel;
        this.connected   = channel.isConnected();
        this.ops         = 0;

        if (connected) {
            try {
                InetSocketAddress localAddr, remoteAddr;

                localAddr = (InetSocketAddress) channel.getLocalAddress();
                remoteAddr = (InetSocketAddress) channel.getRemoteAddress();

                StringBuilder builder = new StringBuilder(128);

                localAddress  = protocol + "://" + localAddr.getHostString()  + ":" + localAddr.getPort();
                remoteAddress = protocol + "://" + remoteAddr.getHostString() + ":" + remoteAddr.getPort();
                str           = "(Local : " + localAddress + " - " + "Remote : " + remoteAddress + ")";
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public abstract ByteBuffer recv();
    public abstract boolean send();
    public abstract ByteBuffer getSendBuf();
    public abstract void releaseResources();

    public boolean isConnected()
    {
        return connected;
    }

    public String getLocalAddress()
    {
        return localAddress;
    }

    public String getRemoteAddress()
    {
        return remoteAddress;
    }

    @Override
    public void onRead()
    {
        owner.handleReadEvent(this);
    }

    @Override
    public void onWrite()
    {
        owner.handleWriteEvent(this);
    }

    public void setOwner(SockOwner owner)
    {
        this.owner = owner;
    }

    public void addOp(int op)
    {
        if ((this.ops & op) == 0) {
            this.ops |= op;
            key.interestOps(ops);
        }
    }

    public void removeOp(int op)
    {
        if ((this.ops & op) != 0) {
            this.ops = (this.ops | ~op);
            key.interestOps(ops);
        }
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

    public boolean isOpen()
    {
        return channel.isOpen();
    }

    public void register(Selector selector, int ops)
    {
        try {
            this.ops = ops;
            this.key = channel.register(selector, ops, this);
        }
        catch (ClosedChannelException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void cancel()
    {
        ops = 0;
        key.cancel();
    }

    public int read(ByteBuffer buf)
    {
        try {
            return channel.read(buf);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int write(ByteBuffer buf)
    {
        try {
            return channel.write(buf);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ByteBuffer getWriteBuf()
    {
        return null;
    }

    public String toString()
    {
        return str;
    }

    public void handshake()
    {

    }
}
