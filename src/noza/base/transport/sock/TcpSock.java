package noza.base.transport.sock;

import java.nio.ByteBuffer;
import java.nio.channels.*;


public class TcpSock extends Sock
{
    private static final String PROTOCOL = "tcp";

    public TcpSock(SockOwner owner, SocketChannel channel)
    {
        super(owner, channel, PROTOCOL);
    }

    @Override
    public void setOwner(SockOwner owner)
    {
        super.setOwner(owner);
        recvBuf = owner.getTempBuf();
    }

    @Override
    public ByteBuffer recv()
    {
        recvBuf.clear();

        int n = read(recvBuf);
        if (n == -1) {
            connected = false;
        }

        return recvBuf;
    }

    @Override
    public ByteBuffer getSendBuf()
    {
        if (sendBuf == null) {
            sendBuf = owner.allocDirectBuf();
        }

        return sendBuf;
    }

    @Override
    public boolean send()
    {
        int n = write(sendBuf);
        if (sendBuf.hasRemaining()) {
            sendBuf.compact();
            addOp(SelectionKey.OP_WRITE);

            return false;
        }

        removeOp(SelectionKey.OP_WRITE);
        owner.freeDirectBuf(sendBuf);

        return true;
    }

    @Override
    public void releaseResources()
    {
        if (sendBuf != null) {
            owner.freeDirectBuf(sendBuf);
        }
    }
}
