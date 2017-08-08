package noza.base.transport.sock;

import noza.core.BufferPool;

import java.nio.ByteBuffer;

public interface SockOwner extends BufferPool
{
    void handleReadEvent(Sock sock);
    void handleWriteEvent(Sock sock);

    BufferPool getPool();

    default ByteBuffer allocBuf()
    {
        return getPool().allocBuf();
    }

    default void freeBuf(ByteBuffer buf)
    {
        getPool().freeBuf(buf);
    }

    default ByteBuffer allocLargeBuf()
    {
        return getPool().allocLargeBuf();
    }

    default void freeLargeBuf(ByteBuffer buf)
    {
        getPool().freeLargeBuf(buf);
    }

    default ByteBuffer allocDirectBuf()
    {
        return getPool().allocDirectBuf();
    }

    default void freeDirectBuf(ByteBuffer buf)
    {
        getPool().freeDirectBuf(buf);
    }

    default ByteBuffer allocLargeDirectBuf()
    {
        return getPool().allocLargeDirectBuf();
    }

    default void freeLargeDirectBuf(ByteBuffer buf)
    {
        getPool().freeLargeDirectBuf(buf);
    }

    default ByteBuffer getTempBuf()
    {
        return getPool().getTempBuf();
    }
}
